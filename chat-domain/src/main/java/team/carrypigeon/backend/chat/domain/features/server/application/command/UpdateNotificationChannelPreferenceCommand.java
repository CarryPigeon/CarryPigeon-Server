package team.carrypigeon.backend.chat.domain.features.server.application.command;

/**
 * 更新频道级通知偏好命令。
 */
public record UpdateNotificationChannelPreferenceCommand(long accountId, long channelId, String mode, Long mutedUntil) {
}
