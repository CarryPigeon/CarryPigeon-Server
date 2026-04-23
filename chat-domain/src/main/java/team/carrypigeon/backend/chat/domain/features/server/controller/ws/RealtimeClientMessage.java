package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;

/**
 * 实时通道客户端消息。
 * 职责：定义 WebSocket 文本命令的最小入站协议结构。
 * 边界：当前阶段只覆盖频道文本消息发送命令。
 *
 * @param type 命令类型
 * @param channelId 频道 ID
 * @param messageType 消息类型
 * @param body 消息正文主体
 * @param payload 结构化载荷
 * @param metadata 元数据
 */
public record RealtimeClientMessage(
        String type,
        Long channelId,
        String messageType,
        String body,
        Map<String, Object> payload,
        Map<String, Object> metadata
) {
}
