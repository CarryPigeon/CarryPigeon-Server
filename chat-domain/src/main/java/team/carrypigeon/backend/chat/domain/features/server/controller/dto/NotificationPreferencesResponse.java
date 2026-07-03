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
    /**
     * 服务级通知偏好响应。
     * 职责：表达当前账号的全局通知模式和静音截止时间。
     */
    public record ServerNotificationPreferenceResponse(
            @Schema(description = "服务端级模式", example = "all") String mode,
            @Schema(description = "静音截止时间", example = "0") long mutedUntil
    ) {
    }

    /**
     * 频道级通知偏好响应。
     * 职责：表达当前账号在单个频道上的通知模式覆盖。
     */
    public record ChannelNotificationPreferenceResponse(
            @Schema(description = "频道 ID", example = "12345") String cid,
            @Schema(description = "频道级模式", example = "inherit") String mode,
            @Schema(description = "静音截止时间", example = "0") long mutedUntil
    ) {
    }
}
