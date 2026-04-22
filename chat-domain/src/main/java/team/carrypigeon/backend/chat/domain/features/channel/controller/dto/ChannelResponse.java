package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import java.time.Instant;

/**
 * 频道协议响应。
 * 职责：向 HTTP 调用方暴露默认频道查询结果。
 * 边界：不承载业务规则与统一响应包装逻辑。
 *
 * @param channelId 频道 ID
 * @param conversationId 会话 ID
 * @param name 频道名称
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelResponse(
        long channelId,
        long conversationId,
        String name,
        String type,
        boolean defaultChannel,
        Instant createdAt,
        Instant updatedAt
) {
}
