package team.carrypigeon.backend.chat.domain.features.server.application.dto;

import java.util.List;

/**
 * 通知偏好应用层结果。
 */
public record NotificationPreferencesResult(
        ServerPreferenceResult server,
        List<ChannelPreferenceResult> channels
) {
    public record ServerPreferenceResult(String mode, long mutedUntil) {
    }

    public record ChannelPreferenceResult(String cid, String mode, long mutedUntil) {
    }
}
