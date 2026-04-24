package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import jakarta.validation.constraints.Positive;

/**
 * 转移频道所有权请求。
 * 职责：承载频道所有权转移 HTTP 入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载治理规则判断。
 *
 * @param targetAccountId 新 OWNER 账户 ID
 */
public record TransferChannelOwnershipRequest(
        @Positive(message = "targetAccountId must be greater than 0")
        long targetAccountId
) {
}
