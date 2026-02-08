package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/channels/{cid}/applications/{application_id}/decisions}.
 */
public record ChannelApplicationDecisionRequest(@NotBlank String decision) {
}

