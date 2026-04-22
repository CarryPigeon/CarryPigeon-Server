package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 频道消息领域模型。
 * 职责：表达 V0 群聊主链路中真实使用的通用消息字段。
 * 边界：当前阶段只内建 text 消息，但模型本身应支持未来消息类型的通用投影。
 *
 * @param messageId 消息 ID
 * @param serverId 服务端 ID
 * @param conversationId 会话 ID
 * @param channelId 频道 ID
 * @param senderId 发送者账户 ID
 * @param messageType 消息类型
 * @param body 消息正文主体
 * @param previewText 面向列表或摘要展示的预览文本
 * @param searchableText 面向搜索索引的可检索文本
 * @param payload 结构化载荷
 * @param metadata 元数据
 * @param status 消息状态
 * @param createdAt 创建时间
 */
public record ChannelMessage(
        long messageId,
        String serverId,
        long conversationId,
        long channelId,
        long senderId,
        String messageType,
        String body,
        String previewText,
        String searchableText,
        String payload,
        String metadata,
        String status,
        Instant createdAt
) {
}
