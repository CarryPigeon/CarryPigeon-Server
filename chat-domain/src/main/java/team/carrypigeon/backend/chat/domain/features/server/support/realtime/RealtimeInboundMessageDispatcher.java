package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.List;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageDeliveryApplicationService;
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
     * @param messageDeliveryApplicationService 消息发送应用服务
     */
    public void dispatch(
            AuthenticatedAccount principal,
            RealtimeClientMessage request,
            MessageDeliveryApplicationService messageDeliveryApplicationService
    ) {
        for (RealtimeInboundMessageHandler handler : handlers) {
            if (handler.supports(request)) {
                handler.handle(principal, request, messageDeliveryApplicationService);
                return;
            }
        }
        throw ProblemException.validationFailed("unsupported realtime message type");
    }
}
