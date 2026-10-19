package team.carrypigeon.backend.chat.domain.config.ws.resume;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;

/**
 * 实时事件续传协调器。
 * 职责：按账户隔离，从事件窗口回放客户端最后确认位置之后的事件。
 * 边界：不认证账户、不修改事件窗口、不管理 Netty 通道生命周期。
 */
@Component
public final class RealtimeResumeCoordinator {

    private final RealtimeEventLog realtimeEventLog;
    private final RealtimeFrameCodec frameCodec;

    public RealtimeResumeCoordinator(RealtimeEventLog realtimeEventLog, RealtimeFrameCodec frameCodec) {
        this.realtimeEventLog = realtimeEventLog;
        this.frameCodec = frameCodec;
    }

    /**
     * 回放客户端最后确认位置之后的可见事件。
     * 失败语义：锚点不存在或已淘汰时下发 {@code resume.failed}，由客户端通过 HTTP 重建状态。
     *
     * @param context Netty 通道上下文
     * @param accountId 账号 ID
     * @param lastEventId 客户端最后确认的事件 ID
     */
    public void replay(ChannelHandlerContext context, long accountId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }
        List<RealtimeFrame.Event> events = realtimeEventLog.eventsAfter(accountId, lastEventId);
        if (events == null) {
            context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(
                    new RealtimeFrame.Ack("resume.failed", null, Map.of("reason", "event_too_old"))
            )));
            return;
        }
        for (RealtimeFrame.Event event : events) {
            context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(event)));
        }
    }
}
