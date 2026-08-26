package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * WebSocket v1 帧编解码器。
 * 职责：解析并校验客户端 envelope，统一构造成功、错误和事件下行帧。
 * 边界：不读取通道状态，不执行认证、续传或 Netty 生命周期操作。
 */
final class RealtimeFrameCodec {

    private final JsonProvider jsonProvider;

    RealtimeFrameCodec(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    RealtimeClientMessage parseClientFrame(String text) {
        return jsonProvider.fromJson(text, RealtimeClientMessage.class);
    }

    void validateClientFrame(RealtimeClientMessage request) {
        if (request == null || request.type() == null || request.type().isBlank()) {
            throw new InvalidClientFrameException("type_blank", "type must not be blank");
        }
    }

    TextWebSocketFrame commandError(String id, String type, String reason, String message) {
        return serverFrame(type, id, null, Map.of("reason", reason, "message", message));
    }

    TextWebSocketFrame eventFrame(RealtimeSessionRegistry.StoredRealtimeEvent event) {
        return serverFrame("event", null, Map.of(
                "event_id", event.eventId(),
                "event_type", event.eventType(),
                "server_time", event.serverTime(),
                "payload", event.payload()
        ), null);
    }

    TextWebSocketFrame serverFrame(String type, String id, Object data, Object error) {
        return new TextWebSocketFrame(jsonProvider.toJson(new RealtimeServerMessage(type, id, data, error)));
    }

    String mapReason(ProblemException exception) {
        return switch (exception.type()) {
            case FORBIDDEN -> switch (exception.reason()) {
                case "authentication_required", "invalid_access_token", "invalid_refresh_token", "invalid_token" ->
                        "unauthorized";
                case "invalid_credentials",
                     "private_channel_required",
                     "channel_invite_forbidden",
                     "channel_profile_forbidden",
                     "channel_role_forbidden",
                     "channel_ownership_forbidden",
                     "channel_ban_forbidden",
                     "channel_pin_forbidden",
                     "channel_membership_required",
                     "not_channel_member",
                     "channel_message_recall_forbidden",
                     "system_channel_membership_required",
                     "system_channel_members_hidden",
                     "system_channel_required" -> "forbidden";
                default -> exception.reason();
            };
            case VALIDATION -> switch (exception.reason()) {
                case "application_already_processed" -> "conflict";
                default -> exception.reason();
            };
            case INTERNAL -> "internal_error";
            default -> exception.reason();
        };
    }

    /**
     * 客户端 envelope 结构无效。
     * 职责：携带内部调试 reason 与对外校验消息，供顶层 handler 保持原关闭行为。
     */
    static final class InvalidClientFrameException extends RuntimeException {

        private final String reason;

        InvalidClientFrameException(String reason, String message) {
            super(message);
            this.reason = reason;
        }

        String reason() {
            return reason;
        }
    }
}
