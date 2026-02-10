package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Required Gate 预检查请求体。
 *
 * @param client 客户端信息。
 */
public record RequiredGateCheckRequest(@NotNull @Valid ClientInfo client) {
}
