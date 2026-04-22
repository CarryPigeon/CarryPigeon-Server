package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 频道消息领域模型。
 * 职责：表达 V0 群聊主链路中真实使用的通用消息字段。
 * 边界：当前阶段只覆盖文本消息，不展开文件消息与插件消息规则。
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
public record ChannelMessage(
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
