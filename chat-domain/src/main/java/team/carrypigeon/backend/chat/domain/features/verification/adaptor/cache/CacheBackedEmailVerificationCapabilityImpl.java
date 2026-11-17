package team.carrypigeon.backend.chat.domain.features.verification.adaptor.cache;

import java.time.Duration;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
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
    // 键包含用途和账号绑定，旧的通用邮箱挑战不再参与校验。
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
    public void issueCode(String email, EmailCodePurpose purpose, Long accountId) {
        String code = String.format(Locale.ROOT, "%06d", CODE_RANDOM.nextInt(1_000_000));
        String cacheKey = challengeKey(email, purpose, accountId);
        String challengeId = UUID.randomUUID().toString();
        CodeChallengeStore store = requireStore();
        try {
            store.replace(cacheKey, challengeId, code, 5, CODE_TTL);
        } catch (CacheServiceException exception) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE, exception);
        }
        try {
            deliverCode(email, code, purpose);
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
    public void verifyCode(String email, String code, EmailCodePurpose purpose, Long accountId) {
        CodeChallengeStore.Result result;
        try {
            result = requireStore().verify(challengeKey(email, purpose, accountId), code);
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

    /** 签发与消费必须构造同一身份，修改邮箱的挑战不能被其它账号消费。 */
    private String challengeKey(String email, EmailCodePurpose purpose, Long accountId) {
        return CACHE_KEY_PREFIX + purpose.name() + ":" + (accountId == null ? "" : accountId) + ":" + email;
    }

    private void deliverCode(String email, String code, EmailCodePurpose purpose) {
        MailSenderService mailSenderService = mailSenderServiceProvider.getIfAvailable();
        if (mailSenderService == null) {
            throw ProblemException.of(VerificationProblem.MAIL_UNAVAILABLE);
        }
        try {
            mailSenderService.send(new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code, purpose)));
        } catch (RuntimeException exception) {
            throw ProblemException.of(VerificationProblem.EMAIL_DELIVERY_FAILED, exception);
        }
    }

    private String buildMailText(String code, EmailCodePurpose purpose) {
        String action = switch (purpose) {
            case REGISTER -> "register a CarryPigeon account";
            case LOGIN -> "sign in to CarryPigeon";
            case CHANGE_EMAIL -> "change your CarryPigeon account email";
        };
        return "Your verification code to " + action + " is: " + code + "\n\n"
                + "This code will expire in 10 minutes.";
    }
}
