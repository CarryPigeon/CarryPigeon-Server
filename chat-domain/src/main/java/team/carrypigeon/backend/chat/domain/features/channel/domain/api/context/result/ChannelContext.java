package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result;

import java.time.Instant;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;

/**
 * 跨 feature 使用的频道成员上下文。
 * 职责：承载调用方鉴权通过后真正需要的频道内事实，避免调用方重复查询频道与成员。
 * 边界：不承载成员角色矩阵以外的治理规则，不承载频道资料，不暴露仓储或协议语义。
 * 约束：禁言不区分限时与永久，均由 {@code mutedUntil} 表达；{@code null} 表示当前未被禁言。
 *
 * @param channelId 频道 ID
 * @param callerRole 调用者在该频道的角色
 * @param mutedUntil 禁言到期时刻；{@code null} 表示当前未被禁言
 */
public record ChannelContext(
        long channelId,
        ChannelMemberRole callerRole,
        Instant mutedUntil
) {

    /**
     * 判断调用者是否具备治理他人内容的资格。
     *
     * @return OWNER 或 ADMIN 时为 {@code true}
     */
    public boolean isModerator() {
        return callerRole == ChannelMemberRole.OWNER || callerRole == ChannelMemberRole.ADMIN;
    }

    /**
     * 判断调用者当前是否处于禁言期。
     *
     * @return 存在禁言到期时刻时为 {@code true}
     */
    public boolean isMuted() {
        return mutedUntil != null;
    }
}
