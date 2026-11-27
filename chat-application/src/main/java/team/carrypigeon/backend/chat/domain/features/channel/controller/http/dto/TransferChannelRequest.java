package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferChannelRequest(
    @Schema(oneOf = {String.class, Long.class}, description = "新所有者的账户 ID")
    @Positive
    @NotNull(message = "New owner cannot be null")
    Long newOwner
) {
}
