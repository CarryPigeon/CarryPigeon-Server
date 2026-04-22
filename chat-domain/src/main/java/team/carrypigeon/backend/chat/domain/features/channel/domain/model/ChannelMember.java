package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道成员领域模型。
 * 职责：表达用户与频道之间最小可验证的成员关系。
 * 边界：当前阶段不承载角色、禁言或邀请等扩展权限语义。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param joinedAt 加入时间
 */
public record ChannelMember(long channelId, long accountId, Instant joinedAt) {
}
