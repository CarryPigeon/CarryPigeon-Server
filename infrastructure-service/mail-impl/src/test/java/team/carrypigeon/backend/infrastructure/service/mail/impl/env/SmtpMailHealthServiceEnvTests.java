package team.carrypigeon.backend.infrastructure.service.mail.impl.env;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.service.mail.impl.smtp.SmtpMailHealthService;

/**
 * Real SMTP environment tests for mail-impl.
 * Contract: SmtpMailHealthService can verify a real SMTP connection when mail env tests are explicitly enabled.
 * Boundary: this test does not send email because the project has no stable test mailbox contract yet.
 */
@Tag("env")
@Tag("env-mail")
class SmtpMailHealthServiceEnvTests {

    /**
     * Verifies that the configured SMTP server accepts a real connection.
     */
    @Test
    @DisplayName("real smtp health check")
    void healthService_realSmtp_returnsAvailable() {
        EnvMailSettings settings = EnvMailSettings.fromEnvironment();
        assumeTrue(settings.enabled(), "set CP_ENV_MAIL_TEST_ENABLED=true or -Dcp.env.mail.test.enabled=true to run env-mail tests");

        SmtpMailHealthService healthService = new SmtpMailHealthService(settings.mailSender());

        assertTrue(healthService.check().available());
    }

    private record EnvMailSettings(
            boolean enabled,
            String host,
            int port,
            String username,
            String password,
            boolean auth,
            boolean startTls
    ) {

        static EnvMailSettings fromEnvironment() {
            return new EnvMailSettings(
                    booleanValue("cp.env.mail.test.enabled", "CP_ENV_MAIL_TEST_ENABLED"),
                    value("cp.env.mail.host", "CP_ENV_MAIL_HOST", ""),
                    Integer.parseInt(value("cp.env.mail.port", "CP_ENV_MAIL_PORT", "587")),
                    value("cp.env.mail.username", "CP_ENV_MAIL_USERNAME", ""),
                    value("cp.env.mail.password", "CP_ENV_MAIL_PASSWORD", ""),
                    booleanValue("cp.env.mail.auth", "CP_ENV_MAIL_AUTH"),
                    booleanValue("cp.env.mail.starttls", "CP_ENV_MAIL_STARTTLS")
            );
        }

        JavaMailSenderImpl mailSender() {
            assumeTrue(host != null && !host.isBlank(), "SMTP host is required for env-mail tests");
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            if (username != null && !username.isBlank()) {
                mailSender.setUsername(username);
            }
            if (password != null && !password.isBlank()) {
                mailSender.setPassword(password);
            }
            Properties properties = mailSender.getJavaMailProperties();
            properties.put("mail.smtp.auth", Boolean.toString(auth));
            properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
            properties.put("mail.smtp.connectiontimeout", "5000");
            properties.put("mail.smtp.timeout", "5000");
            return mailSender;
        }

        private static boolean booleanValue(String propertyName, String envName) {
            return Boolean.parseBoolean(value(propertyName, envName, "false"));
        }

        private static String value(String propertyName, String envName, String defaultValue) {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            return defaultValue;
        }
    }
}
