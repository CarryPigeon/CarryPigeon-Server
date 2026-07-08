package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 通知偏好更新请求。
 */
public record UpdateNotificationPreferenceRequest(
        @Schema(description = "偏好模式", example = "all")
        @NotBlank(message = "mode must not be blank")
        String mode,
        @Schema(description = "静音截止时间，0=永久", example = "0")
        @PositiveOrZero(message = "muted_until must be greater than or equal to 0")
        Long mutedUntil
) {
}
