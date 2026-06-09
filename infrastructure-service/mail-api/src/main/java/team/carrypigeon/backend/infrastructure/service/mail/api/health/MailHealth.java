package team.carrypigeon.backend.infrastructure.service.mail.api.health;

/**
 * 邮件服务健康状态。
 * 职责：表达邮件外部服务当前是否可用。
 * 边界：不暴露 SMTP 连接或厂商实现细节。
 *
 * @param available 邮件服务是否可用
 * @param message 健康状态说明
 */
public record MailHealth(boolean available, String message) {
}
