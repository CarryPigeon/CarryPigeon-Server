package team.carrypigeon.backend.infrastructure.service.mail.impl.health;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealth;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;

/** 启动时检查Mail实现的可用性；检查失败会阻止应用启动。 */
public final class SmtpInitializationChecker implements InitializationChecker {

    private final MailHealthService healthService;

    public SmtpInitializationChecker(MailHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public String name() {
        return "mail";
    }

    @Override
    public InitializationCheckResult check() {
        MailHealth health = healthService.check();
        return new InitializationCheckResult(health.available(), health.message());
    }
}
