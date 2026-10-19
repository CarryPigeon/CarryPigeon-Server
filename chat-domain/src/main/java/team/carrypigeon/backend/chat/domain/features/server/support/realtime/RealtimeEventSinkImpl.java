package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.features.server.domain.capability.RealtimeEventSink;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时事件投递实现。
 * 职责：为事件生成单调递增的标识与服务端时间，先写入续传窗口再向在线连接广播。
 * 边界：不做接收人过滤、不查库、不判断业务语义。
 * 顺序约束：必须先落窗口再广播，保证客户端收到事件后立即断线也能在重连时回放到它。
 */
public class RealtimeEventSinkImpl implements RealtimeEventSink {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventSinkImpl.class);

    private final RealtimeEventLog realtimeEventLog;
    private final RealtimeConnectionIndex realtimeConnectionIndex;
    private final RealtimeFrameCodec frameCodec;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final int maxPayloadBytes;

    public RealtimeEventSinkImpl(
            RealtimeEventLog realtimeEventLog,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec frameCodec,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            int maxPayloadBytes
    ) {
        this.realtimeEventLog = realtimeEventLog;
        this.realtimeConnectionIndex = realtimeConnectionIndex;
        this.frameCodec = frameCodec;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public void deliver(RealtimeEvent event, Collection<Long> recipients) {
        RealtimeFrame.Event frame = new RealtimeFrame.Event(
                idGenerator.nextStringId(),
                event.type(),
                timeProvider.nowMillis(),
                event.payload()
        );
        String frameText = frameCodec.encode(frame);
        int payloadBytes = frameText.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > maxPayloadBytes) {
            // 超大帧视为发射方缺陷：不落窗口、不广播，避免单个事件拖垮连接与内存
            log.error("Realtime event exceeds payload limit, type={}, bytes={}, limit={}",
                    event.type().wireName(), payloadBytes, maxPayloadBytes);
            return;
        }
        realtimeEventLog.append(frame, recipients);
        realtimeConnectionIndex.broadcast(recipients, frameText);
    }
}
