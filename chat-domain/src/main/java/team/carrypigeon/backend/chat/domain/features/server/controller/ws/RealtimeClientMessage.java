package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 实时通道客户端消息。
 * 职责：定义 v1 WebSocket 客户端命令帧结构。
 * 边界：只承载 v1 统一 envelope，不再保留旧入站命令兼容构造。
 *
 * @param type 命令类型
 * @param id 客户端请求 ID
 * @param ts 客户端时间戳
 * @param data 命令载荷
 */
public record RealtimeClientMessage(
        String type,
        String id,
        Long ts,
        Map<String, Object> data
) {

    public Map<String, Object> payload() {
        return objectValue("payload");
    }

    public Map<String, Object> metadata() {
        return objectValue("metadata");
    }

    public Long channelId() {
        return longValue("channel_id");
    }

    public String messageType() {
        return textValue("message_type");
    }

    public String body() {
        return textValue("body");
    }

    public String accessToken() {
        return textValue("access_token");
    }

    public String deviceId() {
        return textValue("device_id");
    }

    public Map<String, Object> resume() {
        return objectValue("resume");
    }

    public String lastEventId() {
        Map<String, Object> resume = resume();
        if (resume == null) {
            return null;
        }
        Object value = resume.get("last_event_id");
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("validation_failed", key + " must be decimal number");
        }
    }

    private String textValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>)) {
            throw ProblemException.validationFailed("validation_failed", key + " must be object");
        }
        return (Map<String, Object>) value;
    }
}
