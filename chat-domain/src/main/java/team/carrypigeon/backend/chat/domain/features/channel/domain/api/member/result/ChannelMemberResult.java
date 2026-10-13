package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;

/**
 * 频道成员结果。
 * 职责：向协议层暴露频道成员列表中的稳定字段。
 * 边界：只承载应用层返回数据，不承载成员治理逻辑。
 *
 * @param accountId 成员账户 ID
 * @param username 成员用户名（同时作为公开昵称）
 * @param avatarUrl 成员头像地址
 * @param role 固定角色
 * @param joinedAt 加入时间
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
