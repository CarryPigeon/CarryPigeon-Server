package team.carrypigeon.backend.chat.domain.config.ws.resume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 验证实时事件窗口的保留、淘汰与续传定位语义。
 */
class RealtimeEventLogTests {

    private static final long RETENTION_MILLIS = Duration.ofHours(1).toMillis();
    private static final RealtimeEventType MESSAGE_CREATED = new RealtimeEventType("message.created");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    /**
     * 验证事件会进入每个接收账号各自的窗口。
     * 判定方式：以该事件自身作为锚点时必须命中，因此返回非 null 的空列表。
     */
    @Test
    void append_singleEvent_entersEveryRecipientWindow() {
        RealtimeEventLog eventLog = eventLog(2, 10);

        eventLog.append(event("9001"), List.of(1001L, 1002L));

        assertTrue(eventLog.eventsAfter(1001L, "9001").isEmpty());
        assertTrue(eventLog.eventsAfter(1002L, "9001").isEmpty());
    }

    /**
     * 验证按锚点只返回其后的新事件。
     */
    @Test
    void eventsAfter_knownAnchor_returnsOnlyNewerEvents() {
        RealtimeEventLog eventLog = eventLog(10, 10);
        eventLog.append(event("9001"), List.of(1001L));
        eventLog.append(event("9002"), List.of(1001L));
        eventLog.append(event("9003"), List.of(1001L));

        assertEquals(List.of("9002", "9003"), eventIds(eventLog.eventsAfter(1001L, "9001")));
    }

    /**
     * 验证锚点已是最后一条事件时返回空列表而不是 null。
     */
    @Test
    void eventsAfter_anchorIsLatest_returnsEmptyList() {
        RealtimeEventLog eventLog = eventLog(10, 10);
        eventLog.append(event("9001"), List.of(1001L));

        List<RealtimeFrame.Event> events = eventLog.eventsAfter(1001L, "9001");

        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    /**
     * 验证未知锚点表示不可续传，返回 null。
     */
    @Test
    void eventsAfter_unknownAnchor_returnsNull() {
        RealtimeEventLog eventLog = eventLog(10, 10);
        eventLog.append(event("9001"), List.of(1001L));

        assertNull(eventLog.eventsAfter(1001L, "older-than-window"));
    }

    /**
     * 验证从未建档的账号表示不可续传，返回 null。
     */
    @Test
    void eventsAfter_unknownAccount_returnsNull() {
        RealtimeEventLog eventLog = eventLog(10, 10);

        assertNull(eventLog.eventsAfter(1001L, "9001"));
    }

    /**
     * 验证空白锚点视为没有续传需求，返回空列表。
     */
    @Test
    void eventsAfter_blankAnchor_returnsEmptyList() {
        RealtimeEventLog eventLog = eventLog(10, 10);
        eventLog.append(event("9001"), List.of(1001L));

        assertTrue(eventLog.eventsAfter(1001L, " ").isEmpty());
    }

    /**
     * 验证超出保留时间的事件被清理，窗口随之失效。
     */
    @Test
    void eventsAfter_eventsOlderThanRetention_returnsNull() {
        RealtimeEventLog eventLog = eventLog(10, 10);
        eventLog.append(event("9001"), List.of(1001L));

        clock.advance(Duration.ofHours(2));

        assertNull(eventLog.eventsAfter(1001L, "9001"));
    }

    /**
     * 验证单账号条数上限按先进先出淘汰最旧事件。
     */
    @Test
    void append_exceedsPerAccountLimit_evictsOldestEvent() {
        RealtimeEventLog eventLog = eventLog(10, 2);
        eventLog.append(event("9001"), List.of(1001L));
        eventLog.append(event("9002"), List.of(1001L));
        eventLog.append(event("9003"), List.of(1001L));

        assertNull(eventLog.eventsAfter(1001L, "9001"));
        assertEquals(List.of("9003"), eventIds(eventLog.eventsAfter(1001L, "9002")));
    }

    /**
     * 验证账号数达上限且存在全过期窗口时，回收过期窗口为新账号腾出位置。
     */
    @Test
    void append_accountLimitReachedWithExpiredWindows_reclaimsSlotForNewAccount() {
        RealtimeEventLog eventLog = eventLog(2, 10);
        eventLog.append(event("9001"), List.of(1001L));
        eventLog.append(event("9002"), List.of(1002L));

        clock.advance(Duration.ofHours(2));

        eventLog.append(event("9003"), List.of(1003L));

        assertNull(eventLog.eventsAfter(1001L, "9001"));
        assertNull(eventLog.eventsAfter(1002L, "9002"));
        assertTrue(eventLog.eventsAfter(1003L, "9003").isEmpty());
    }

    /**
     * 验证账号数达上限且无可回收窗口时不为新账号建档，已有窗口不受影响。
     */
    @Test
    void append_accountLimitReachedWithoutExpiredWindows_rejectsNewAccount() {
        RealtimeEventLog eventLog = eventLog(1, 10);
        eventLog.append(event("9001"), List.of(1001L));

        eventLog.append(event("9002"), List.of(1002L));

        assertNull(eventLog.eventsAfter(1002L, "9002"));
        assertNotNull(eventLog.eventsAfter(1001L, "9001"));
    }

    private RealtimeEventLog eventLog(int maxAccounts, int maxEventsPerAccount) {
        return new RealtimeEventLog(
                new TimeProviderImpl(clock),
                RETENTION_MILLIS,
                maxEventsPerAccount,
                maxAccounts
        );
    }

    private RealtimeFrame.Event event(String eventId) {
        return new RealtimeFrame.Event(eventId, MESSAGE_CREATED, clock.currentMillis(), Map.of("cid", "1"));
    }

    private List<String> eventIds(List<RealtimeFrame.Event> events) {
        return events.stream().map(RealtimeFrame.Event::eventId).toList();
    }

    /**
     * 可推进的测试时钟。
     * 职责：让事件窗口的时间窗口与过期判定可被确定性地驱动。
     */
    private static final class MutableClock extends Clock {

        private final ZoneId zone = ZoneOffset.UTC;
        private Instant current;

        private MutableClock(Instant start) {
            this.current = start;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId requestedZone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        private long currentMillis() {
            return current.toEpochMilli();
        }
    }
}
