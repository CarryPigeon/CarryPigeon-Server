package team.carrypigeon.backend.infrastructure.service.mail.impl.smtp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SmtpMailHealthService 契约测试。
 * 职责：验证 SMTP 健康检查会把连接测试结果收口为稳定健康状态。
 * 边界：不连接真实 SMTP，只验证结果映射语义。
 */
@Tag("contract")
class SmtpMailHealthServiceTests {

    /**
     * 验证连接测试成功时返回 available=true。
     */
    @Test
    @DisplayName("check successful connection returns available health")
    void check_successfulConnection_returnsAvailableHealth() {
        SmtpMailHealthService service = new SmtpMailHealthService(new SuccessfulJavaMailSenderImpl());

        MailHealth health = service.check();

        assertTrue(health.available());
        assertEquals("mail service is available", health.message());
    }

    /**
     * 验证连接测试失败时返回 available=false。
     */
    @Test
    @DisplayName("check failed connection returns unavailable health")
    void check_failedConnection_returnsUnavailableHealth() {
        SmtpMailHealthService service = new SmtpMailHealthService(new FailingJavaMailSenderImpl());

        MailHealth health = service.check();

        assertFalse(health.available());
        assertEquals("mail service is unavailable: smtp down", health.message());
    }

    /**
     * 验证 SMTP 异常消息会被转换为有界单行诊断。
     */
    @Test
    @DisplayName("check unsafe failure returns sanitized diagnostic")
    void check_unsafeFailure_returnsSanitizedDiagnostic() {
        SmtpMailHealthService service = new SmtpMailHealthService(
                new MessageFailingJavaMailSenderImpl("first\r\nsecond" + "x".repeat(600))
        );

        MailHealth health = service.check();

        assertFalse(health.message().contains("\r"));
        assertFalse(health.message().contains("\n"));
        assertTrue(health.message().endsWith("..."));
    }

    /**
     * `SuccessfulJavaMailSenderImpl` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class SuccessfulJavaMailSenderImpl extends JavaMailSenderImpl {

        @Override
        public void testConnection() {
        }
    }

    /**
     * `FailingJavaMailSenderImpl` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class FailingJavaMailSenderImpl extends JavaMailSenderImpl {

        @Override
        public void testConnection() {
            throw new IllegalStateException("smtp down");
        }
    }

    /**
     * 可配置异常消息的 SMTP 测试替身。
     * 职责：验证不可信底层诊断文本的日志安全边界。
     */
    private static final class MessageFailingJavaMailSenderImpl extends JavaMailSenderImpl {

        private final String message;

        private MessageFailingJavaMailSenderImpl(String message) {
            this.message = message;
        }

        @Override
        public void testConnection() {
            throw new IllegalStateException(message);
        }
    }
}
