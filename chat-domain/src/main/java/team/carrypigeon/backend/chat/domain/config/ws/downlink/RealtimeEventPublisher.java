package team.carrypigeon.backend.chat.domain.config.ws.downlink;

import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.config.ws.transport.NettyRealtimeConnectionManager;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时事件发布实现。
 * 职责：实现固定基建的 {@link RealtimeEventApi}；规范化接收人、生成事件标识与服务端时间，
 */
@Component
@Slf4j
public class RealtimeEventPublisher implements RealtimeEventApi {

    private final NettyRealtimeConnectionManager cpNettyRealtimeConnectionManager;
    private final RealtimeFrameCodec frameCodec;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final int maxPayloadBytes;

    public RealtimeEventPublisher(
            NettyRealtimeConnectionManager cpNettyRealtimeConnectionManager,
            RealtimeFrameCodec frameCodec,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            RealtimeServerProperties properties
    ) {
        this.cpNettyRealtimeConnectionManager = cpNettyRealtimeConnectionManager;
        this.frameCodec = frameCodec;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.maxPayloadBytes = properties.getMaxPayloadByte();
    }

    @Override
    public void publish(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        List<Long> recipients = event.recipientAccountIds();
        if (recipients.isEmpty()) {
            return;
        }
        try {
            RealtimeFrame.Event frame = new RealtimeFrame.Event(
                idGenerator.nextStringId(),
                event.type(),
                timeProvider.nowMillis(),
                event.payload()
            );
            String frameText = frameCodec.encode(frame);
            int payloadBytes = frameText.getBytes(StandardCharsets.UTF_8).length;
            if (payloadBytes > maxPayloadBytes) {
                log.error("Realtime event exceeds payload limit, type={}, bytes={}, limit={}",
                    event.type().wireName(), payloadBytes, maxPayloadBytes);
                return;
            }
            cpNettyRealtimeConnectionManager.broadcast(recipients, frameText);
        } catch (RuntimeException exception) {
            // 事务已提交，推送失败不得回传为业务失败；丢失的事件由客户端通过 HTTP 全量同步补齐
            log.error("Failed to deliver realtime event, type={}", event.type().wireName(), exception);
        }
    }
}
