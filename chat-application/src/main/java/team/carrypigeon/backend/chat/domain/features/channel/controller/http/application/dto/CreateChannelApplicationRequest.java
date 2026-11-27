package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 创建入群申请请求。
 * 职责：承载提交入群申请时附带的申请理由。
 * 边界：只做字段承载与协议校验，不表达申请状态、重复申请与审批权限规则。
 */
public record CreateChannelApplicationRequest(
        @Schema(description = "申请理由", example = "hi")
        @NotNull(message = "reason must not be null")
        String reason
) {
}
