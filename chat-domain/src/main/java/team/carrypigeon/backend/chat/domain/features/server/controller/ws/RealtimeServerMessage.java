package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

/**
 * 实时通道消息。
 * 职责：定义 Netty WebSocket 文本消息的最小协议结构。
 * 边界：当前阶段只表达基础事件，不绑定具体聊天业务字段。
 *
 * @param type 消息类型
 * @param sessionId 会话标识
 * @param timestamp 时间戳
 * @param content 文本内容
 */
public record RealtimeServerMessage(String type, String sessionId, long timestamp, String content) {
}
