package team.carrypigeon.backend.chat.domain.features.message.application.dto;

import java.time.Instant;

/**
 * 频道消息应用层结果。
 * 职责：向协议层暴露稳定消息字段。
 * 边界：不承载协议包装逻辑。
 *
 * @param messageId 消息 ID
 * @param serverId 服务端 ID
 * @param conversationId 会话 ID
 * @param channelId 频道 ID
 * @param senderId 发送者账户 ID
 * @param messageType 消息类型
 * @param body 消息正文主体
 * @param previewText 预览文本
 * @param payload 结构化载荷
 * @param metadata 元数据
 * @param status 消息状态
 * @param createdAt 创建时间
 */
public record ChannelMessageResult(
        long messageId,
        String serverId,
        long conversationId,
        long channelId,
        long senderId,
        String messageType,
        String body,
        String previewText,
        String payload,
        String metadata,
        String status,
        Instant createdAt
) {
}
