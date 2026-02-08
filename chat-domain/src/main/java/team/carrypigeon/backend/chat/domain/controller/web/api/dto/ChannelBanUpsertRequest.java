package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PUT /api/channels/{cid}/bans/{uid}}.
 */
public record ChannelBanUpsertRequest(@NotNull Long until, String reason) {
}

