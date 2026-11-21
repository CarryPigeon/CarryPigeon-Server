package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TransferChannelRequest(
    @Schema(description = "The new owner of the channel")
    @NotNull(message = "New owner cannot be null")
    String newOwner
) {
}
