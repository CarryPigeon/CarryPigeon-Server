package team.carrypigeon.backend.chat.domain.features.auth.support.verification;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 基于缓存的邮箱验证码服务。
 * 职责：把邮箱验证码持久化到共享缓存，并完成验证码邮件投递编排。
 * 边界：缓存与邮件客户端的具体实现细节停留在基础设施层。
 */
public class CacheBackedEmailVerificationCodeService implements EmailVerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_KEY_PREFIX = "auth:email-code:";
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final CacheService cacheService;
    private final MailSenderService mailSenderService;

    public CacheBackedEmailVerificationCodeService(CacheService cacheService) {
        this(cacheService, null);
    }

    public CacheBackedEmailVerificationCodeService(CacheService cacheService, MailSenderService mailSenderService) {
        this.cacheService = cacheService;
        this.mailSenderService = mailSenderService;
    }

    @Override
    public void issueCode(String email) {
        String normalizedEmail = normalize(email);
        String code = generateCode();
        String cacheKey = cacheKey(normalizedEmail);
        cacheService.set(cacheKey, code, CODE_TTL);
        try {
            deliverCode(normalizedEmail, code);
        } catch (RuntimeException exception) {
            cacheService.delete(cacheKey);
            throw exception;
        }
    }

    @Override
    public void verifyCode(String email, String code) {
        if (code == null || code.isBlank()) {
            throw ProblemException.validationFailed("email code is invalid");
        }
        if (!cacheService.consumeIfEquals(cacheKey(email), code)) {
            throw ProblemException.validationFailed("email code is invalid");
        }
    }

    private String cacheKey(String email) {
        return CACHE_KEY_PREFIX + normalize(email);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    private void deliverCode(String email, String code) {
        if (mailSenderService == null) {
            throw ProblemException.fail("mail_service_unavailable", "mail service is unavailable");
        }
        try {
            mailSenderService.send(new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code)));
        } catch (RuntimeException exception) {
            throw ProblemException.fail("email_delivery_failed", "failed to deliver verification email");
        }
    }

    private String buildMailText(String code) {
        return "Your CarryPigeon verification code is: " + code + "\n\n"
                + "This code will expire in 10 minutes.";
    }
}
