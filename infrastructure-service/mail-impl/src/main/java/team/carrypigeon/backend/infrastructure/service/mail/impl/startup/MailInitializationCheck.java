package team.carrypigeon.backend.infrastructure.service.mail.impl.startup;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealth;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;

/**
 * 邮件初始化检查。
 * 职责：将邮件健康检查适配为共享启动检查契约。
 * 边界：只负责契约转换，不暴露 SMTP 连接细节。
 */
public class MailInitializationCheck implements InitializationCheck {

    private final MailHealthService mailHealthService;

    public MailInitializationCheck(MailHealthService mailHealthService) {
        this.mailHealthService = mailHealthService;
    }

    /**
     * 返回启动检查项名称。
     */
    @Override
    public String name() {
        return "mail";
    }

    /**
     * 执行邮件健康检查并转换为统一启动检查结果。
     */
    @Override
    public InitializationCheckResult check() {
        MailHealth health = mailHealthService.check();
        return health.available()
                ? InitializationCheckResult.passed(health.message())
                : InitializationCheckResult.failed(health.message());
    }
}
