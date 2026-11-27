package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 审批入群申请请求。
 * 职责：承载审批决定字段。
 * 边界：只做字段承载与协议校验，决定值的合法集合与状态流转由领域层判定。
 */
public record DecideChannelApplicationRequest(
        @Schema(description = "审批决定", example = "approve")
        @NotBlank(message = "decision must not be blank")
        String decision
) {
}
