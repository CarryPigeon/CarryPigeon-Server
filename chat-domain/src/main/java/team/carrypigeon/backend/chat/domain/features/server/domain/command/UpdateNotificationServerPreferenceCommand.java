package team.carrypigeon.backend.chat.domain.features.server.domain.command;

/**
 * 更新服务端级通知偏好命令。
 */
public record UpdateNotificationServerPreferenceCommand(long accountId, String mode, Long mutedUntil) {
}
