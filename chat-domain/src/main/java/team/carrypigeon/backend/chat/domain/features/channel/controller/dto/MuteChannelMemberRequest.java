package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import jakarta.validation.constraints.Positive;

/**
 * 禁言频道成员请求。
 * 职责：承载频道成员禁言 HTTP 入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载治理规则判断。
 *
 * @param durationSeconds 禁言持续秒数
 */
public record MuteChannelMemberRequest(
        @Positive(message = "durationSeconds must be greater than 0")
        long durationSeconds
) {
}
