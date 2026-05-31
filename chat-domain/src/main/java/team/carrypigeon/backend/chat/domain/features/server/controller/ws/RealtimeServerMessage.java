package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

/**
 * 实时通道消息。
 * 职责：定义 v1 WebSocket 服务端帧结构。
 * 边界：统一承载命令响应、事件 envelope 与错误输出，不扩展更高阶订阅模型。
 *
 * @param type 消息类型
 * @param data 事件载荷
 * @param id 命令关联 ID
 * @param error 错误对象
 */
public record RealtimeServerMessage(String type, String id, Object data, Object error) {

    public RealtimeServerMessage(String type, String sessionId, long timestamp, Object data) {
        this(type, sessionId, data, null);
    }
}
