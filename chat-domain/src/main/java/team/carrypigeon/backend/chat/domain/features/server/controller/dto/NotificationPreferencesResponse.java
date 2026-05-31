package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 通知偏好响应。
 */
public record NotificationPreferencesResponse(
        ServerNotificationPreferenceResponse server,
        List<ChannelNotificationPreferenceResponse> channels
) {
    public record ServerNotificationPreferenceResponse(
            @Schema(description = "服务端级模式", example = "all") String mode,
            @Schema(description = "静音截止时间", example = "0") long mutedUntil
    ) {
    }

    public record ChannelNotificationPreferenceResponse(
            @Schema(description = "频道 ID", example = "12345") String cid,
            @Schema(description = "频道级模式", example = "inherit") String mode,
            @Schema(description = "静音截止时间", example = "0") long mutedUntil
    ) {
    }
}
