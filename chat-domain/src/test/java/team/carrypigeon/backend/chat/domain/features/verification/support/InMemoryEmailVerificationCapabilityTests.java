package team.carrypigeon.backend.chat.domain.features.verification.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * InMemoryEmailVerificationCapability 契约测试。
 * 职责：验证进程内验证码服务会同步发信，并在成功校验后立即失效。
 * 边界：不验证真实邮件投递或多进程共享能力。
 */
@Tag("contract")
class InMemoryEmailVerificationCapabilityTests {

    /**
     * 验证签发验证码时会发出邮件，并允许后续成功校验。
     */
    @Test
    @DisplayName("issue and verify code sends mail and invalidates code after success")
    void issueAndVerifyCode_sendsMailAndInvalidatesCodeAfterSuccess() {
        RecordingMailSenderService mailSenderService = new RecordingMailSenderService();
        InMemoryEmailVerificationCapability service = new InMemoryEmailVerificationCapability(
                new TimeProvider(Clock.fixed(Instant.parse("2026-06-02T06:00:00Z"), ZoneOffset.UTC)),
                mailSenderService
        );

        service.issueCode(" Carry-User@Example.com ");

        assertEquals("carry-user@example.com", mailSenderService.lastCommand.to());
        assertEquals("CarryPigeon verification code", mailSenderService.lastCommand.subject());
        assertNotNull(mailSenderService.lastCommand.text());
        String code = extractCode(mailSenderService.lastCommand.text());

        service.verifyCode("carry-user@example.com", code);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.verifyCode("carry-user@example.com", code)
        );

        assertEquals("email code is invalid", exception.getMessage());
    }

    /**
     * 验证发信失败时不会留下可用验证码。
     */
    @Test
    @DisplayName("issue code mail failure rolls back issued code")
    void issueCode_mailFailure_rollsBackIssuedCode() {
        InMemoryEmailVerificationCapability service = new InMemoryEmailVerificationCapability(
                new TimeProvider(Clock.fixed(Instant.parse("2026-06-02T06:00:00Z"), ZoneOffset.UTC)),
                command -> {
                    throw new IllegalStateException("smtp down");
                }
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.issueCode("user@example.com")
        );

        assertEquals("email_delivery_failed", exception.reason());
    }

    /**
     * 验证未装配邮件服务时会明确拒绝签发，避免返回成功但实际未投递。
     */
    @Test
    @DisplayName("issue code without mail sender throws mail service unavailable")
    void issueCode_withoutMailSender_throwsMailServiceUnavailable() {
        InMemoryEmailVerificationCapability service = new InMemoryEmailVerificationCapability(
                new TimeProvider(Clock.fixed(Instant.parse("2026-06-02T06:00:00Z"), ZoneOffset.UTC))
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.issueCode("user@example.com")
        );

        assertEquals("mail_service_unavailable", exception.reason());
    }

    private String extractCode(String text) {
        String prefix = "Your CarryPigeon verification code is: ";
        int start = text.indexOf(prefix);
        return text.substring(start + prefix.length(), start + prefix.length() + 6);
    }

    /**
     * `RecordingMailSenderService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingMailSenderService implements MailSenderService {

        private MailSendCommand lastCommand;

        @Override
        public void send(MailSendCommand command) {
            this.lastCommand = command;
        }
    }
}
