package team.carrypigeon.backend.chat.domain.features.verification.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.EmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EmailVerificationDomainApi 契约测试。
 * 职责：验证邮箱规范化后才委托 verification 内部 capability。
 */
@Tag("contract")
class EmailVerificationDomainApiTests {

    /**
     * 验证签发和校验入口都会在委托内部 capability 前规范化邮箱。
     */
    @Test
    @DisplayName("issue and verify mixed case email normalizes boundary input")
    void issueAndVerify_mixedCaseEmail_normalizesBoundaryInput() {
        RecordingCapability capability = new RecordingCapability();
        EmailVerificationDomainApi api = new EmailVerificationDomainApi(capability);

        api.issueCode(new IssueEmailVerificationCodeCommand(" User@Example.COM "));
        api.verifyCode(new VerifyEmailVerificationCodeCommand(" User@Example.COM ", "123456"));

        assertEquals("user@example.com", capability.issuedEmail);
        assertEquals("user@example.com", capability.verifiedEmail);
    }

    private static final class RecordingCapability implements EmailVerificationCapability {
        private String issuedEmail;
        private String verifiedEmail;
        @Override public void issueCode(String email) { issuedEmail = email; }
        @Override public void verifyCode(String email, String code) { verifiedEmail = email; }
    }
}
