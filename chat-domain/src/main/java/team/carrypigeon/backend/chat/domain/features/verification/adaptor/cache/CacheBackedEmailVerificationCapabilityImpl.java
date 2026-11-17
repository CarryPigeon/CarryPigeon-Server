package team.carrypigeon.backend.chat.domain.features.verification.adaptor.cache;

import java.time.Duration;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.CacheBackedEmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CodeChallengeStore;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 基于缓存的邮箱验证码 capability 实现。
 * 职责：把邮箱验证码持久化到共享缓存，并完成验证码邮件投递编排。
 * 边界：缓存与邮件客户端的具体实现细节停留在基础设施层。
 */
@Component
public class CacheBackedEmailVerificationCapabilityImpl implements CacheBackedEmailVerificationCapability {

    // 认证凭证使用 JDK 安全随机源，不以时间戳或用户身份作为种子。
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    // 与旧字符串验证码使用不同的键，直接切换挑战格式，旧验证码不再参与校验。
    private static final String CACHE_KEY_PREFIX = "auth:email-challenge:";
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final ObjectProvider<CodeChallengeStore> challengeStoreProvider;
    private final ObjectProvider<MailSenderService> mailSenderServiceProvider;

    public CacheBackedEmailVerificationCapabilityImpl(
            ObjectProvider<CodeChallengeStore> challengeStoreProvider,
            ObjectProvider<MailSenderService> mailSenderServiceProvider
    ) {
        this.challengeStoreProvider = challengeStoreProvider;
        this.mailSenderServiceProvider = mailSenderServiceProvider;
    }

    @Override
    public void issueCode(String email) {
        String code = String.format(Locale.ROOT, "%06d", CODE_RANDOM.nextInt(1_000_000));
        String cacheKey = CACHE_KEY_PREFIX + email;
        String challengeId = UUID.randomUUID().toString();
        CodeChallengeStore store = requireStore();
        try {
            store.replace(cacheKey, challengeId, code, 5, CODE_TTL);
        } catch (CacheServiceException exception) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE, exception);
        }
        try {
            deliverCode(email, code);
        } catch (RuntimeException exception) {
            // 旧批次投递失败时只能撤销自己，不能误删已签发的新挑战。
            try {
                store.discard(cacheKey, challengeId);
            } catch (CacheServiceException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    @Override
    public void verifyCode(String email, String code) {
        CodeChallengeStore.Result result;
        try {
            result = requireStore().verify(CACHE_KEY_PREFIX + email, code);
        } catch (CacheServiceException exception) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE, exception);
        }
        if (result == CodeChallengeStore.Result.VERIFIED) {
            return;
        }
        if (result == CodeChallengeStore.Result.EXHAUSTED) {
            throw ProblemException.of(VerificationProblem.TOO_MANY_REQUESTS);
        }
        throw ProblemException.of(VerificationProblem.INVALID_CODE, "email code is invalid");
    }

    /**
     * 获取验证码缓存能力。
     * 缺少挑战存储时返回稳定错误；不对其它依赖缓存的功能作启动保证。
     */
    private CodeChallengeStore requireStore() {
        CodeChallengeStore store = challengeStoreProvider.getIfAvailable();
        if (store == null) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE);
        }
        return store;
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
