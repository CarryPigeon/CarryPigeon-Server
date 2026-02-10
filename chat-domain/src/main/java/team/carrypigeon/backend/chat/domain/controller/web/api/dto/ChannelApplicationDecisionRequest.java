package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 入群申请审批请求体。
 *
 * @param decision 审批结果（如 `approve` / `reject`）。
 */
public record ChannelApplicationDecisionRequest(@NotBlank String decision) {
}
