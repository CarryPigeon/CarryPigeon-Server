package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import jakarta.validation.constraints.Positive;

/**
 * 邀请频道成员请求。
 * 职责：承载频道邀请 HTTP 入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载治理规则判断。
 *
 * @param inviteeAccountId 被邀请账户 ID
 */
public record InviteChannelMemberRequest(
        @Positive(message = "inviteeAccountId must be greater than 0")
        long inviteeAccountId
) {
}
