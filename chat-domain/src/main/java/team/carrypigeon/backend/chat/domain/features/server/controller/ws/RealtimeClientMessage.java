package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 实时通道客户端消息。
 * 职责：定义 v1 WebSocket 客户端命令帧结构。
 * 边界：只承载 v1 统一 envelope，不保留旧入站命令构造。
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

    /**
     * 从统一 data envelope 中读取可选数字字段。
     * 约束：兼容 JSON number 与数字字符串；无法解析时返回 v1 协议校验错误。
     *
     * @param key data 字段名
     * @return long 值，缺失时为 null
     */
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
    /**
     * 从统一 data envelope 中读取可选对象字段。
     * 失败语义：字段存在但不是 JSON object 时返回 v1 协议校验错误。
     *
     * @param key data 字段名
     * @return 对象字段，缺失时为 null
     */
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
