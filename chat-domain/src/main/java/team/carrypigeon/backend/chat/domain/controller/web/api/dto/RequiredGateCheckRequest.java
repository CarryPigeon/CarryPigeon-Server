package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Required gate check body for {@code POST /api/gates/required/check}.
 */
public record RequiredGateCheckRequest(@NotNull @Valid ClientInfo client) {
}
