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

    /**
     * 为目标邮箱签发新的验证码。
     * 输入：邮箱地址。
     * 副作用：覆盖共享缓存中该邮箱先前签发的验证码，并投递验证码邮件。
     * 失败语义：邮件服务不可用或投递失败时删除本次缓存验证码后抛出领域问题异常。
     *
     * @param email 目标邮箱
     */
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

    /**
     * 校验邮箱验证码是否有效。
     * 输入：邮箱地址与用户提交的验证码。
     * 副作用：校验成功后通过缓存原子消费验证码，避免重复使用。
     *
     * @param email 目标邮箱
     * @param code 用户提交的验证码
     */
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

    /**
     * 投递验证码邮件。
     * 副作用：调用邮件服务发送验证码；邮件服务缺失或发送失败时抛出统一领域问题。
     *
     * @param email 已规范化目标邮箱
     * @param code 六位验证码
     */
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
