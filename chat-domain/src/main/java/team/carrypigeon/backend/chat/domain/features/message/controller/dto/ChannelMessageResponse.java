package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import java.time.Instant;

/**
 * 频道消息协议响应。
 * 职责：向 HTTP 调用方暴露历史消息的稳定字段。
 * 边界：不承载业务规则与统一响应包装逻辑。
 *
 * @param messageId 消息 ID
 * @param serverId 服务端 ID
 * @param conversationId 会话 ID
 * @param channelId 频道 ID
 * @param senderId 发送者账户 ID
 * @param messageType 消息类型
 * @param content 文本内容
 * @param payload 结构化载荷
 * @param metadata 元数据
 * @param status 消息状态
 * @param createdAt 创建时间
 */
public record ChannelMessageResponse(
        long messageId,
        String serverId,
        long conversationId,
        long channelId,
        long senderId,
        String messageType,
        String content,
        String payload,
        String metadata,
        String status,
        Instant createdAt
) {
}
