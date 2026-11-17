package team.carrypigeon.backend.chat.domain.config.ws.downlink;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.config.ws.transport.NettyRealtimeConnectionManager;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
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
        if (event == null || event.recipientAccountIds().isEmpty()) {
            return;
        }
        try {
            String frameText = encode(event.type(), event.payload());
            if (frameText != null) {
                cpNettyRealtimeConnectionManager.broadcast(event.recipientAccountIds(), frameText);
            }
        } catch (RuntimeException exception) {
            log.error("Failed to deliver realtime event, type={}", event.type().wireName(), exception);
        }
    }

    @Override
    public void publishToChannel(long channelId, RealtimeEventType type, Object payload) {
        try {
            String frameText = encode(type, payload);
            if (frameText != null) {
                cpNettyRealtimeConnectionManager.broadcastToChannel(channelId, frameText);
            }
        } catch (RuntimeException exception) {
            // 业务事务已经提交；索引或授权查询失败时停止本次推送，客户端按频道序号补拉。
            log.error("Failed to deliver channel event, channelId={}, type={}", channelId, type.wireName(), exception);
        }
    }

    /** 同一事件只编码一次，协议帧字节上限对账号与频道两种路由一致生效。 */
    private String encode(RealtimeEventType type, Object payload) {
        String frameText = frameCodec.encode(new RealtimeFrame.Event(
                idGenerator.nextStringId(), type, timeProvider.nowMillis(), payload));
        int payloadBytes = frameText.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > maxPayloadBytes) {
            log.error("Realtime event exceeds payload limit, type={}, bytes={}, limit={}",
                    type.wireName(), payloadBytes, maxPayloadBytes);
            return null;
        }
        return frameText;
    }
}
