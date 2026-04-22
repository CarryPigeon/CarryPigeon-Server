package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

/**
 * 实时通道客户端消息。
 * 职责：定义 WebSocket 文本命令的最小入站协议结构。
 * 边界：当前阶段只覆盖频道文本消息发送命令。
 *
 * @param type 命令类型
 * @param channelId 频道 ID
 * @param content 文本内容
 */
public record RealtimeClientMessage(String type, Long channelId, String content) {
}
