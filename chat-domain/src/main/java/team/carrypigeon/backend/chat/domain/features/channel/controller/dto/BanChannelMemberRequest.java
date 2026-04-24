package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 封禁频道成员请求。
 * 职责：承载频道成员封禁 HTTP 入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载治理规则判断。
 *
 * @param targetAccountId 目标账户 ID
 * @param reason 封禁原因
 * @param durationSeconds 封禁持续秒数；为空表示无限期
 */
public record BanChannelMemberRequest(
        @Positive(message = "targetAccountId must be greater than 0")
        long targetAccountId,
        @Size(max = 256, message = "reason length must be less than or equal to 256")
        String reason,
        @Positive(message = "durationSeconds must be greater than 0")
        Long durationSeconds
) {
}
