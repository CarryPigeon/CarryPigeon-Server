package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.List;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 实时入站消息分发器。
 * 职责：从已注册处理器中选择一个处理当前 WebSocket 入站命令。
 * 边界：只负责分派，不承载具体消息业务规则。
 */
public class RealtimeInboundMessageDispatcher {

    private final List<RealtimeInboundMessageHandler> handlers;

    public RealtimeInboundMessageDispatcher(List<RealtimeInboundMessageHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    /**
     * 分派并处理入站消息。
     *
     * @param principal 当前认证主体
     * @param request 入站消息
     * @param channelMessagePublishingApi 频道消息发布领域 API
     */
    public void dispatch(
            AuthenticatedAccount principal,
            RealtimeClientMessage request,
            ChannelMessagePublishingApi channelMessagePublishingApi
    ) {
        for (RealtimeInboundMessageHandler handler : handlers) {
            if (handler.supports(request)) {
                handler.handle(principal, request, channelMessagePublishingApi);
                return;
            }
        }
        throw ProblemException.validationFailed("unsupported realtime message type");
    }
}
