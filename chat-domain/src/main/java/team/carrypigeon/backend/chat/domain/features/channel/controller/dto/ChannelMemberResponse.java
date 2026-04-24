package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import java.time.Instant;

/**
 * 频道成员响应。
 * 职责：向 HTTP 协议层返回成员列表的稳定字段。
 * 边界：只承载协议输出，不承载成员治理逻辑。
 *
 * @param accountId 成员账户 ID
 * @param nickname 成员昵称
 * @param avatarUrl 成员头像地址
 * @param role 固定角色
 * @param joinedAt 加入时间
 * @param mutedUntil 禁言截止时间
 */
public record ChannelMemberResponse(
        long accountId,
        String nickname,
        String avatarUrl,
        String role,
        Instant joinedAt,
        Instant mutedUntil
) {
}
