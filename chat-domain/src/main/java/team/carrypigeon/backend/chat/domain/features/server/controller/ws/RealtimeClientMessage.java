package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import java.util.Map;

/**
 * 实时通道客户端消息。
 * 职责：定义 WebSocket 统一消息发送命令的入站协议结构。
 * 边界：当前协议仅承载 `send_channel_message` 发送命令，不提供 HTTP 发送入口。
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
