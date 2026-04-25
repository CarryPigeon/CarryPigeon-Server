package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.util.AttributeKey;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;

/**
 * 实时通道会话上下文。
 * 职责：为当前 WebSocket 会话提供统一的会话、认证与日志上下文键定义。
 * 边界：这里只维护通道 attribute key，不承载具体会话业务逻辑。
 */
public final class RealtimeChannelSession {

    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("cp.realtime.session-id");
    public static final AttributeKey<String> TRACE_ID_KEY = AttributeKey.valueOf("cp.realtime.trace-id");
    public static final AttributeKey<String> REQUEST_ID_KEY = AttributeKey.valueOf("cp.realtime.request-id");
    public static final AttributeKey<String> ROUTE_KEY = AttributeKey.valueOf("cp.realtime.route");
    public static final AttributeKey<AuthenticatedPrincipal> AUTHENTICATED_PRINCIPAL_KEY =
            AttributeKey.valueOf("cp.realtime.authenticated-principal");

    private RealtimeChannelSession() {
    }
}
