package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道成员领域模型。
 * 职责：表达账号与频道之间的活跃成员关系，以及该关系上的固定角色。
 * 边界：只表示已加入频道的活跃成员；账号退出或被移出后记录即被删除，因此不承载历史成员关系；申请记录与禁言记录由独立模型表达。
 *
 * @param id 成员关系主键；更新角色时必须沿用既有值，重新发号会导致更新命中不到任何行
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param role 成员在频道内的固定角色
 * @param joinedAt 成员加入频道的时间
 */
public record ChannelMember(
    long id,
    long channelId,
    long accountId,
    ChannelMemberRole role,
    Instant joinedAt
) {
}
