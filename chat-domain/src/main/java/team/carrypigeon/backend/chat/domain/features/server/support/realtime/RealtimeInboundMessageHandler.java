package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;

/**
 * 实时入站消息处理器。
 * 职责：按协议类型与消息类型处理具体 WebSocket 入站命令。
 * 边界：处理器只负责单一入站命令分支，不负责通道生命周期与统一错误回写。
 */
public interface RealtimeInboundMessageHandler {

    /**
     * 判断是否支持当前入站消息。
     *
     * @param request 入站消息
     * @return 支持时返回 true
     */
    boolean supports(RealtimeClientMessage request);

    /**
     * 处理当前入站消息。
     *
     * @param principal 当前认证主体
     * @param request 入站消息
     * @param messageApplicationService 消息应用服务
     */
    void handle(
            AuthenticatedPrincipal principal,
            RealtimeClientMessage request,
            MessageApplicationService messageApplicationService
    );
}
