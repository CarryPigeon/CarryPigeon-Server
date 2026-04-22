package team.carrypigeon.backend.chat.domain.features.channel.application.dto;

import java.time.Instant;

/**
 * 频道应用层结果。
 * 职责：向协议层暴露默认频道查询所需的稳定字段。
 * 边界：不承载协议层包装逻辑。
 *
 * @param channelId 频道 ID
 * @param conversationId 会话 ID
 * @param name 频道名称
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelResult(
        long channelId,
        long conversationId,
        String name,
        String type,
        boolean defaultChannel,
        Instant createdAt,
        Instant updatedAt
) {
}
