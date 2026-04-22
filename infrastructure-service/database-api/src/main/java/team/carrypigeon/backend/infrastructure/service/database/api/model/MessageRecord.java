package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 消息数据库记录契约。
 * 职责：为 chat-domain 与 database-impl 之间传递通用消息字段。
 * 边界：这里只表达数据库服务契约，不承载消息业务规则。
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
public record MessageRecord(
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
