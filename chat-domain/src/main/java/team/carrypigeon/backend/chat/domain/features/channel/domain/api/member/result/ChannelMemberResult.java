package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;

/**
 * 频道成员结果投影。
 * 职责：向协议层暴露成员列表所需的账号身份、角色与加入时间字段。
 * 边界：只承载展示字段，不承载成员治理逻辑；用户资料来自 user feature 的投影，缺失时对应字段为空。
 *
 * @param accountId 成员账户 ID
 * @param username 成员用户名，同时作为公开昵称
 * @param avatarUrl 成员头像相对路径；资料缺失时可能为空
 * @param role 成员在频道内的固定角色名，大写形式
 * @param joinedAt 成员加入频道的时间
 */
public record ChannelMemberResult(
        long accountId,
        String username,
        String avatarUrl,
        String role,
        java.time.Instant joinedAt
) {

    public static ChannelMemberResult from(ChannelMember member, UserProfileResult user) {
        return new ChannelMemberResult(
                member.accountId(),
                user.username(),
                user.avatarUrl(),
                member.role().name(),
                member.joinedAt()
        );
    }
}
