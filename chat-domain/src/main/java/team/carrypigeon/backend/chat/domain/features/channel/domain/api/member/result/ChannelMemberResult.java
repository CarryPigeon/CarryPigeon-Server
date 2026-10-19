package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.MemberProfile;

/**
 * 频道成员结果投影。
 * 职责：向协议层暴露成员列表所需的账号身份、角色与加入时间字段。
 * 边界：只承载展示字段，不承载成员治理逻辑；资料缺失时对应字段为空，由协议层决定如何展示。
 *
 * @param accountId 成员账户 ID
 * @param username 成员用户名，同时作为公开昵称；资料缺失时为空
 * @param avatarUrl 成员头像相对路径；资料缺失时为空
 * @param role 成员在频道内的固定角色名，大写形式
 * @param joinedAt 成员加入频道的时间
 */
public record ChannelMemberResult(
        long accountId,
        String username,
        String avatarUrl,
        String role,
        Instant joinedAt
) {

    /**
     * 由成员关系与展示资料构造投影。
     *
     * @param member 频道成员关系
     * @param profile 成员展示资料，允许为空
     * @return 成员投影
     */
    public static ChannelMemberResult from(ChannelMember member, MemberProfile profile) {
        return new ChannelMemberResult(
                member.accountId(),
                profile == null ? null : profile.username(),
                profile == null ? null : profile.avatarUrl(),
                member.role().name(),
                member.joinedAt()
        );
    }
}
