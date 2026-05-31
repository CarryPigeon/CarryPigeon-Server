package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建频道请求。
 * 职责：承载 `POST /api/channels` 的最小字段。
 * 边界：当前创建 private channel，并接收 `docs/t` 所要求的 brief/avatar 输入。
 */
public record CreateChannelRequest(
        @Schema(description = "频道名称", example = "General")
        @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name length must be less than or equal to 128")
        String name,
        @Schema(description = "频道简介", example = "")
        @NotNull(message = "brief must not be null")
        String brief,
        @Schema(description = "频道头像相对路径", example = "")
        @NotNull(message = "avatar must not be null")
        String avatar
) {
}
