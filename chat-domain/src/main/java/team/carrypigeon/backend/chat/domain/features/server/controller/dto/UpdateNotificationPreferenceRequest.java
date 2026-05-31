package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知偏好更新请求。
 */
public record UpdateNotificationPreferenceRequest(
        @Schema(description = "偏好模式", example = "all") String mode,
        @Schema(description = "静音截止时间，0=永久", example = "0") Long mutedUntil
) {
}
