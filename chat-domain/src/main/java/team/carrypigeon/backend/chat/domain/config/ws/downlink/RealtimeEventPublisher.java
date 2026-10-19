package team.carrypigeon.backend.chat.domain.config.ws.downlink;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.config.ws.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeEventLog;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时事件发布实现。
 * 职责：实现固定基建的 {@link RealtimeEventApi}；规范化接收人、生成事件标识与服务端时间，
 * 先写入可续传窗口，再向在线连接广播，并把投递失败隔离在业务事务之外。
 * 边界：不查询仓储、不判断业务语义；载荷由发射方定义，本类不解释其内容。
 * 顺序约束：必须先落窗口再广播，保证客户端收到事件后立即断线也能在重连时回放到它。
 */
@Component
@Slf4j
public class RealtimeEventPublisher implements RealtimeEventApi {

    private final RealtimeEventLog realtimeEventLog;
    private final RealtimeConnectionIndex realtimeConnectionIndex;
    private final RealtimeFrameCodec frameCodec;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final int maxPayloadBytes;

    public RealtimeEventPublisher(
            RealtimeEventLog realtimeEventLog,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec frameCodec,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            RealtimeServerProperties properties
    ) {
        this.realtimeEventLog = realtimeEventLog;
        this.realtimeConnectionIndex = realtimeConnectionIndex;
        this.frameCodec = frameCodec;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.maxPayloadBytes = properties.maxPayloadBytes();
    }

    /**
     * 发布一次实时事件。
     * 语义：尽力而为。接收人为空时不投递；投递失败只记录日志，不影响调用方已提交的事务。
     *
     * @param event 待发布事件
     */
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
            realtimeEventLog.append(frame, recipients);
            realtimeConnectionIndex.broadcast(recipients, frameText);
        } catch (RuntimeException exception) {
            // 事务已提交，推送失败不得回传为业务失败；丢失的事件由客户端通过 HTTP 全量同步补齐
            log.error("Failed to deliver realtime event, type={}", event.type().wireName(), exception);
        }
    }
}
