package team.carrypigeon.backend.chat.domain.features.auth.support.verification;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CacheBackedEmailVerificationCodeService 契约测试。
 * 职责：验证邮箱验证码会持久化到共享缓存，并在成功校验后立即失效。
 * 边界：不验证具体缓存实现，只验证上层服务对 CacheService 的交互语义。
 */
@Tag("contract")
class CacheBackedEmailVerificationCodeServiceTests {

    @Test
    @DisplayName("issue and verify code normalized email deletes cache entry")
    void issueAndVerifyCode_normalizedEmail_deletesCacheEntry() {
        RecordingCacheService cacheService = new RecordingCacheService();
        RecordingMailSenderService mailSenderService = new RecordingMailSenderService();
        CacheBackedEmailVerificationCodeService service = new CacheBackedEmailVerificationCodeService(cacheService, mailSenderService);

        service.issueCode(" Carry-User@Example.com ");

        assertEquals("auth:email-code:carry-user@example.com", cacheService.lastSetKey);
        assertEquals(Duration.ofMinutes(10), cacheService.lastSetTtl);
        assertNotNull(cacheService.lastSetValue);
        assertEquals("carry-user@example.com", mailSenderService.lastCommand.to());
        assertEquals("CarryPigeon verification code", mailSenderService.lastCommand.subject());

        service.verifyCode("carry-user@example.com", cacheService.lastSetValue);

        assertEquals("auth:email-code:carry-user@example.com", cacheService.lastDeletedKey);
        assertFalse(cacheService.exists("auth:email-code:carry-user@example.com"));
    }

    @Test
    @DisplayName("verify code mismatch throws validation problem")
    void verifyCode_mismatch_throwsValidationProblem() {
        RecordingCacheService cacheService = new RecordingCacheService();
        CacheBackedEmailVerificationCodeService service = new CacheBackedEmailVerificationCodeService(cacheService, new RecordingMailSenderService());
        cacheService.set("auth:email-code:user@example.com", "123456", Duration.ofMinutes(10));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.verifyCode("user@example.com", "654321")
        );

        assertEquals("email code is invalid", exception.getMessage());
    }

    /**
     * 验证发信失败时会回滚缓存中的验证码，避免留下用户收不到的可用验证码。
     */
    @Test
    @DisplayName("issue code mail failure deletes cached code")
    void issueCode_mailFailure_deletesCachedCode() {
        RecordingCacheService cacheService = new RecordingCacheService();
        CacheBackedEmailVerificationCodeService service = new CacheBackedEmailVerificationCodeService(
                cacheService,
                command -> {
                    throw new IllegalStateException("smtp down");
                }
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.issueCode("user@example.com")
        );

        assertEquals("email_delivery_failed", exception.reason());
        assertFalse(cacheService.exists("auth:email-code:user@example.com"));
        assertEquals("auth:email-code:user@example.com", cacheService.lastDeletedKey);
    }

    /**
     * 验证未装配邮件服务时会回滚缓存中的验证码，并返回稳定错误语义。
     */
    @Test
    @DisplayName("issue code without mail sender deletes cached code and throws unavailable problem")
    void issueCode_withoutMailSender_deletesCachedCodeAndThrowsUnavailableProblem() {
        RecordingCacheService cacheService = new RecordingCacheService();
        CacheBackedEmailVerificationCodeService service = new CacheBackedEmailVerificationCodeService(cacheService);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.issueCode("user@example.com")
        );

        assertEquals("mail_service_unavailable", exception.reason());
        assertFalse(cacheService.exists("auth:email-code:user@example.com"));
        assertEquals("auth:email-code:user@example.com", cacheService.lastDeletedKey);
    }

    private static final class RecordingCacheService implements CacheService {

        private final Map<String, String> values = new HashMap<>();
        private String lastSetKey;
        private String lastSetValue;
        private Duration lastSetTtl;
        private String lastDeletedKey;

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public void set(String key, String value, Duration ttl) {
            lastSetKey = key;
            lastSetValue = value;
            lastSetTtl = ttl;
            values.put(key, value);
        }

        @Override
        public void delete(String key) {
            lastDeletedKey = key;
            values.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }

    private static final class RecordingMailSenderService implements MailSenderService {

        private MailSendCommand lastCommand;

        @Override
        public void send(MailSendCommand command) {
            this.lastCommand = command;
        }
    }
}
