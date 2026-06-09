package team.carrypigeon.backend.infrastructure.service.mail.impl.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealth;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MailInitializationCheck 契约测试。
 * 职责：验证邮件健康检查会被稳定映射到共享启动检查结果。
 * 边界：只验证契约转换，不验证 SMTP 连接细节。
 */
@Tag("contract")
class MailInitializationCheckTests {

    /**
     * 验证可用健康状态会映射为通过结果。
     */
    @Test
    @DisplayName("check available health returns passed result")
    void check_availableHealth_returnsPassedResult() {
        MailInitializationCheck check = new MailInitializationCheck(() -> new MailHealth(true, "ok"));

        InitializationCheckResult result = check.check();

        assertEquals(true, result.passed());
        assertEquals("ok", result.message());
    }

    /**
     * 验证不可用健康状态会映射为失败结果。
     */
    @Test
    @DisplayName("check unavailable health returns failed result")
    void check_unavailableHealth_returnsFailedResult() {
        MailHealthService healthService = () -> new MailHealth(false, "smtp down");
        MailInitializationCheck check = new MailInitializationCheck(healthService);

        InitializationCheckResult result = check.check();

        assertEquals(false, result.passed());
        assertEquals("smtp down", result.message());
    }
}
