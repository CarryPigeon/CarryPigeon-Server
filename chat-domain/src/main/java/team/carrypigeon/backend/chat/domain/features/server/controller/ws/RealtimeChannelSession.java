package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.util.AttributeKey;

/**
 * 实时通道会话上下文。
 * 职责：为当前 WebSocket 会话提供统一的会话键定义。
 * 边界：当前阶段只管理最小会话标识，不扩展认证态和用户态。
 */
public final class RealtimeChannelSession {

    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("cp.realtime.session-id");

    private RealtimeChannelSession() {
    }
}
