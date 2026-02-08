package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/channels}.
 */
public record ChannelCreateRequest(
        @NotBlank String name,
        String brief,
        String avatar
) {
}

