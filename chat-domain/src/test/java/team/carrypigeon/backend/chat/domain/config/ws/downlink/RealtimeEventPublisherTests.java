package team.carrypigeon.backend.chat.domain.config.ws.downlink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.config.ws.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeEventLog;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 验证实时事件发布的投递顺序、失败隔离与载荷上限约束。
 */
class RealtimeEventPublisherTests {

    private static final long NOW_MILLIS = 1700000000000L;
    private static final String GENERATED_EVENT_ID = "42";
    private static final RealtimeEventType TYPE = new RealtimeEventType("channel.changed");

    private final RealtimeEventLog eventLog = new RealtimeEventLog(new FixedTimeProvider(), 3_600_000L, 100, 100);
    private final RecordingConnectionIndex connectionIndex = new RecordingConnectionIndex();

    /**
     * 验证接收人为空时不编码、不落窗口、不广播。
     */
    @Test
    void publish_emptyRecipients_doesNotDeliver() {
        RealtimeEventPublisher publisher = publisher(4096);

        publisher.publish(new RealtimeEvent(TYPE, Map.of(), List.of()));

        assertTrue(connectionIndex.broadcastTexts.isEmpty());
        assertNull(eventLog.eventsAfter(1001L, GENERATED_EVENT_ID));
    }

    /**
     * 验证合法事件先落窗口再广播，且事件标识与服务端时间被写入帧。
     */
    @Test
    void publish_validEvent_appendsToWindowThenBroadcasts() {
        RealtimeEventPublisher publisher = publisher(4096);

        publisher.publish(new RealtimeEvent(TYPE, Map.of("cid", "1"), List.of(1001L)));

        assertEquals(1, connectionIndex.broadcastTexts.size());
        String frame = connectionIndex.broadcastTexts.get(0);
        assertTrue(frame.contains(GENERATED_EVENT_ID));
        assertTrue(frame.contains("channel.changed"));
        assertTrue(frame.contains(String.valueOf(NOW_MILLIS)));
        // 锚点命中说明事件确实已进入可续传窗口
        assertEquals(List.of(), eventLog.eventsAfter(1001L, GENERATED_EVENT_ID));
    }

    /**
     * 验证超过载荷上限的事件不落窗口也不广播，避免超大帧拖垮连接。
     */
    @Test
    void publish_oversizedEvent_dropsWithoutWindowOrBroadcast() {
        RealtimeEventPublisher publisher = publisher(10);

        publisher.publish(new RealtimeEvent(TYPE, Map.of("cid", "1"), List.of(1001L)));

        assertTrue(connectionIndex.broadcastTexts.isEmpty());
        assertNull(eventLog.eventsAfter(1001L, GENERATED_EVENT_ID));
    }

    /**
     * 验证广播失败被吞掉，不回传给已提交事务的调用方，且事件仍留在窗口内。
     */
    @Test
    void publish_broadcastFails_isSwallowedAndEventStaysInWindow() {
        RealtimeEventPublisher publisher = new RealtimeEventPublisher(
                eventLog,
                new FailingConnectionIndex(),
                codec(),
                idGenerator(),
                new FixedTimeProvider(),
                properties(4096)
        );

        assertDoesNotThrow(() -> publisher.publish(new RealtimeEvent(TYPE, Map.of(), List.of(1001L))));
        assertNotNull(eventLog.eventsAfter(1001L, GENERATED_EVENT_ID));
    }

    private RealtimeEventPublisher publisher(int maxPayloadBytes) {
        return new RealtimeEventPublisher(
                eventLog,
                connectionIndex,
                codec(),
                idGenerator(),
                new FixedTimeProvider(),
                properties(maxPayloadBytes)
        );
    }

    private static RealtimeFrameCodec codec() {
        return new RealtimeFrameCodec(new JsonProviderImpl(new ObjectMapper()));
    }

    private static IdGenerator idGenerator() {
        return () -> Long.parseLong(GENERATED_EVENT_ID);
    }

    private static RealtimeServerProperties properties(int maxPayloadBytes) {
        return new RealtimeServerProperties(
                true,
                "127.0.0.1",
                8081,
                "/ws",
                0,
                0,
                10,
                60,
                Duration.ofHours(1),
                100,
                100,
                maxPayloadBytes
        );
    }

    /**
     * 记录广播文本的在线索引替身。
     * 说明：只关心发布链路是否把编码结果交给广播出口，因此不建立真实 Netty 通道。
     */
    private static final class RecordingConnectionIndex extends RealtimeConnectionIndex {

        private final List<String> broadcastTexts = new ArrayList<>();

        @Override
        public void broadcast(Collection<Long> accountIds, String frameText) {
            broadcastTexts.add(frameText);
        }
    }

    /**
     * 广播必定失败的在线索引替身。
     * 说明：用于验证推送失败被隔离在业务事务之外。
     */
    private static final class FailingConnectionIndex extends RealtimeConnectionIndex {

        @Override
        public void broadcast(Collection<Long> accountIds, String frameText) {
            throw new IllegalStateException("broadcast failed");
        }
    }

    /**
     * 固定时间提供器。
     * 说明：让帧内 server_time 与事件窗口过期判定可被确定性地断言。
     */
    private static final class FixedTimeProvider implements TimeProvider {

        @Override
        public Instant nowInstant() {
            return Instant.ofEpochMilli(NOW_MILLIS);
        }

        @Override
        public long nowMillis() {
            return NOW_MILLIS;
        }

        @Override
        public LocalDateTime nowLocalDateTime() {
            return LocalDateTime.ofInstant(nowInstant(), ZoneId.of("UTC"));
        }

        @Override
        public ZoneId zoneId() {
            return ZoneId.of("UTC");
        }
    }
}
