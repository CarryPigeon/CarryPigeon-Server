package team.carrypigeon.backend.infrastructure.basic.realtime.uplink;

import java.util.Map;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeProtocolException;

/**
 * 实时通道客户端消息。
 * 职责：定义客户端命令帧的统一 envelope，并提供可选的通用字段读取。
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

    /**
     * 读取认证凭证。
     *
     * @return access token，缺失时为 null
     */
    public String accessToken() {
        return textValue("access_token");
    }

    /**
     * 读取客户端命令载荷中的文本字段。
     *
     * @param key data 字段名
     * @return 字段文本，缺失时为 null
     */
    public String textValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从统一 data envelope 中读取可选对象字段。
     * 失败语义：字段存在但不是 JSON object 时抛出协议异常，由传输分派层转换为命令错误帧。
     *
     * @param key data 字段名
     * @return 对象字段，缺失时为 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> objectValue(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new RealtimeProtocolException("validation_failed", key + " must be object");
        }
        return (Map<String, Object>) value;
    }
}
