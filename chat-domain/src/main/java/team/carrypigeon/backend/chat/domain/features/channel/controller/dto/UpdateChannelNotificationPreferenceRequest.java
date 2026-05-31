package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新频道通知偏好请求。
 */
public record UpdateChannelNotificationPreferenceRequest(
        @Schema(description = "通知模式", example = "inherit") String mode,
        @Schema(description = "静音截止时间，0=永久", example = "0") Long mutedUntil
) {
}
