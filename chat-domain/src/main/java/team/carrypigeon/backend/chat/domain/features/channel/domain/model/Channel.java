package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道领域模型。
 * 职责：表达 V0 群聊主链路中实际使用的频道稳定语义。
 * 边界：当前阶段只覆盖群聊频道，不展开复杂频道管理规则。
 *
 * @param id 频道 ID
 * @param conversationId 对应会话 ID，V0 中保持最小建模字段
 * @param name 频道名称
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record Channel(
        long id,
        long conversationId,
        String name,
        String type,
        boolean defaultChannel,
        Instant createdAt,
        Instant updatedAt
) {
}
