package team.carrypigeon.backend.chat.domain.features.verification.adaptor.cache;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 基于缓存的邮箱验证码 capability 实现。
 * 职责：把邮箱验证码持久化到共享缓存，并完成验证码邮件投递编排。
 * 边界：缓存与邮件客户端的具体实现细节停留在基础设施层。
 */
@Component
public class CacheBackedEmailVerificationCapabilityImpl implements team.carrypigeon.backend.chat.domain.features.verification.domain.capability.CacheBackedEmailVerificationCapability {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_KEY_PREFIX = "auth:email-code:";
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final ObjectProvider<MailSenderService> mailSenderServiceProvider;

    public CacheBackedEmailVerificationCapabilityImpl(
            ObjectProvider<CacheService> cacheServiceProvider,
            ObjectProvider<MailSenderService> mailSenderServiceProvider
    ) {
        this.cacheServiceProvider = cacheServiceProvider;
        this.mailSenderServiceProvider = mailSenderServiceProvider;
    }

    @Override
    public void issueCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String cacheKey = CACHE_KEY_PREFIX + email;
        CacheService cacheService = requireCache();
        cacheService.set(cacheKey, code, CODE_TTL);
        try {
            deliverCode(email, code);
        } catch (RuntimeException exception) {
            cacheService.delete(cacheKey);
            throw exception;
        }
    }

    @Override
    public void verifyCode(String email, String code) {
        if (!requireCache().consumeIfEquals(CACHE_KEY_PREFIX + email, code)) {
            throw ProblemException.of(VerificationProblem.INVALID_CODE, "email code is invalid");
        }
    }

    /**
     * 获取验证码缓存能力。
     * 说明：缓存模块允许在部署时关闭；关闭后应用仍可启动，仅验证码相关接口返回稳定错误。
     */
    private CacheService requireCache() {
        CacheService cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService == null) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE);
        }
        return cacheService;
    }

    private void deliverCode(String email, String code) {
        MailSenderService mailSenderService = mailSenderServiceProvider.getIfAvailable();
        if (mailSenderService == null) {
            throw ProblemException.of(VerificationProblem.MAIL_UNAVAILABLE);
        }
        try {
            mailSenderService.send(new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code)));
        } catch (RuntimeException exception) {
            throw ProblemException.of(VerificationProblem.EMAIL_DELIVERY_FAILED, exception);
        }
    }

    private String buildMailText(String code) {
        // TODO 改进为通过配置文件配置
        return "Your CarryPigeon verification code is: " + code + "\n\n"
                + "This code will expire in 10 minutes.";
    }
}
