package team.carrypigeon.backend.chat.domain.ws.downlink;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.ws.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.ws.transport.NettyRealtimeConnectionManager;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrameCodec;
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
            cpNettyRealtimeConnectionManager.broadcast(event.recipientAccountIds(), frameText);
        } catch (RuntimeException exception) {
            log.atError()
                .addKeyValue("event_type", event.type().wireName())
                .setCause(exception)
                .log("实时事件发布失败");
        }
    }

    @Override
    public void publishToChannel(long channelId, RealtimeEventType type, Object payload) {
        try {
            String frameText = encode(type, payload);
            cpNettyRealtimeConnectionManager.broadcastToChannel(channelId, frameText);
        } catch (RuntimeException exception) {
            // 业务事务已经提交；索引或授权查询失败时停止本次推送，客户端通过频道事件游标补拉。
            log.atWarn()
                .addKeyValue("channel_id", channelId)
                .addKeyValue("event_type", type.wireName())
                .setCause(exception)
                .log("频道实时推送失败，客户端可通过游标补拉");
        }
    }

    @Override
    public void publishToChannel(long channelId, RealtimeFrame.Event event) {
        cpNettyRealtimeConnectionManager.broadcastToChannel(channelId, encode(event));
    }

    /** 同一事件只编码一次，协议帧字节上限对账号与频道两种路由一致生效。 */
    private String encode(RealtimeEventType type, Object payload) {
        return encode(new RealtimeFrame.Event(idGenerator.nextStringId(), type, timeProvider.nowMillis(), payload));
    }

    private String encode(RealtimeFrame.Event event) {
        String frameText = frameCodec.encode(event);
        int payloadBytes = frameText.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > maxPayloadBytes) {
            throw new IllegalStateException("Realtime event exceeds payload limit: type=" + event.type().wireName()
                    + ", bytes=" + payloadBytes + ", limit=" + maxPayloadBytes);
        }
        return frameText;
    }
}
