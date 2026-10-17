package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;

/**
 * WebSocket v1 帧编解码器。
 * 职责：解析并校验客户端 envelope，统一构造成功、错误和事件下行帧。
 * 边界：不读取通道状态，不执行认证、续传或 Netty 生命周期操作。
 */
final class RealtimeFrameCodec {

    private final JsonProviderImpl jsonProvider;

    RealtimeFrameCodec(JsonProviderImpl jsonProvider) {
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

    /**
     * 取领域问题在实时通道上的对外原因。
     * 约束：直接复用异常携带的错误码，保证 HTTP 与 WebSocket 两个通道的对外原因完全一致。
     *
     * @param exception 领域问题异常
     * @return 对外原因
     */
    String mapReason(ProblemException exception) {
        return exception.reason();
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
