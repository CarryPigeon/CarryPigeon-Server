package team.carrypigeon.backend.infrastructure.service.mail.api.health;

/**
 * 邮件健康检查抽象。
 * 职责：为启动层和上层模块提供邮件服务可用性判断入口。
 * 边界：具体 SMTP 检查逻辑由 mail-impl 承担。
 */
public interface MailHealthService {

    /**
     * 检查邮件服务当前健康状态。
     *
     * @return 邮件健康状态
     */
    MailHealth check();
}
