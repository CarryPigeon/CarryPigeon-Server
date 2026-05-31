package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;

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

    @SuppressWarnings("unchecked")
    public Map<String, Object> payload() {
        return data == null ? null : (Map<String, Object>) data.get("payload");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> metadata() {
        return data == null ? null : (Map<String, Object>) data.get("metadata");
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> resume() {
        return data == null ? null : (Map<String, Object>) data.get("resume");
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
        return Long.parseLong(String.valueOf(value));
    }

    private String textValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
