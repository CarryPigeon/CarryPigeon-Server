package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

/**
 * 实时事件续传协调器。
 * 职责：根据客户端最后确认的事件 ID 推进按账户隔离的 replay 流程。
 * 边界：不认证账户、不修改事件窗口，也不管理 Netty channel 生命周期。
 */
final class RealtimeResumeCoordinator {

    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final RealtimeFrameCodec frameCodec;

    RealtimeResumeCoordinator(
            RealtimeSessionRegistry realtimeSessionRegistry,
            RealtimeFrameCodec frameCodec
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.frameCodec = frameCodec;
    }

    /**
     * 回放客户端最后确认位置之后的可见事件。
     * 失败语义：锚点不存在或已淘汰时下发 `resume.failed`，由客户端通过 HTTP 重建状态。
     */
    void replay(ChannelHandlerContext context, long accountId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }
        List<RealtimeSessionRegistry.StoredRealtimeEvent> events = realtimeSessionRegistry
                .eventsAfter(accountId, lastEventId);
        if (events == null) {
            context.writeAndFlush(frameCodec.serverFrame(
                    "resume.failed", null, Map.of("reason", "event_too_old"), null
            ));
            return;
        }
        for (RealtimeSessionRegistry.StoredRealtimeEvent event : events) {
            context.writeAndFlush(frameCodec.eventFrame(event));
        }
    }
}
