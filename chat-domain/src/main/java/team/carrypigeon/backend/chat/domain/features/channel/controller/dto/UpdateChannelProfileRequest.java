package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 更新频道资料请求。
 * 职责：承载 `PATCH /api/channels/{cid}` 的最小资料字段。
 * 边界：仅承载频道名称与简介更新，不扩展额外资料字段。
 */
public record UpdateChannelProfileRequest(
        @Schema(description = "频道名称", example = "New Name")
        @NotBlank(message = "name must not be blank")
        String name,
        @Schema(description = "频道简介", example = "...")
        @NotNull(message = "brief must not be null")
        String brief
) {
}
