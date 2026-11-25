package team.carrypigeon.backend.chat.domain.features.verification.adaptor.cache;

import java.time.Duration;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.CacheBackedEmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;
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
@Slf4j
public class CacheBackedEmailVerificationCapabilityImpl implements CacheBackedEmailVerificationCapability {

    // 认证凭证使用 JDK 安全随机源，不以时间戳或用户身份作为种子。
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    // 键包含用途和账号绑定，旧的通用邮箱挑战不再参与校验。
    private static final String CACHE_KEY_PREFIX = "auth:email-challenge:";
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final CodeChallengeStore challengeStore;
    private final MailSenderService mailSenderService;

    public CacheBackedEmailVerificationCapabilityImpl(
            CodeChallengeStore challengeStore,
            MailSenderService mailSenderService
    ) {
        this.challengeStore = challengeStore;
        this.mailSenderService = mailSenderService;
    }

    @Override
    public void issueCode(String email, EmailCodePurpose purpose, Long accountId) {
        String code = String.format(Locale.ROOT, "%06d", CODE_RANDOM.nextInt(1_000_000));
        String cacheKey = challengeKey(email, purpose, accountId);
        String challengeId = UUID.randomUUID().toString();
        try {
            challengeStore.replace(cacheKey, challengeId, code, 5, CODE_TTL);
        } catch (CacheServiceException exception) {
            throw ProblemException.of(VerificationProblem.CACHE_UNAVAILABLE, exception);
        }
        try {
            CompletionStage<Void> delivery = deliverCode(email, code, purpose);
            // HTTP 成功只表示任务已受理；SMTP 异步失败时仅作废当前 challenge，不能影响之后重发的批次。
            delivery.whenComplete((ignored, failure) -> completeDelivery(cacheKey, challengeId, purpose, accountId, failure));
        } catch (RuntimeException exception) {
            // 同步投递失败也登记为本批次失败，与异步结果使用同一作废约束。
            completeDelivery(cacheKey, challengeId, purpose, accountId, exception);
            throw exception;
        }
    }

    @Override
    public void verifyCode(String email, String code, EmailCodePurpose purpose, Long accountId) {
        CodeChallengeStore.Result result;
        try {
            result = challengeStore.verify(challengeKey(email, purpose, accountId), code);
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

    /** 签发与消费必须构造同一身份，修改邮箱的挑战不能被其它账号消费。 */
    private String challengeKey(String email, EmailCodePurpose purpose, Long accountId) {
        return CACHE_KEY_PREFIX + purpose.name() + ":" + (accountId == null ? "" : accountId) + ":" + email;
    }

    private CompletionStage<Void> deliverCode(String email, String code, EmailCodePurpose purpose) {
        try {
            CompletionStage<Void> result = mailSenderService.send(new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code, purpose)));
            if (result == null) {
                throw ProblemException.of(VerificationProblem.EMAIL_DELIVERY_FAILED, "mail sender returned no completion");
            }
            return result;
        } catch (RuntimeException exception) {
            throw ProblemException.of(VerificationProblem.EMAIL_DELIVERY_FAILED, exception);
        }
    }

    /** 结果和缓存写入分别观测；缓存短时故障不能吞掉真实 SMTP 结果，也不能在已返回的 HTTP 上补报。 */
    private void completeDelivery(String cacheKey, String challengeId,
            EmailCodePurpose purpose, Long accountId, Throwable failure) {
        if (failure == null) {
            AuditLogs.success("auth.code_delivery", accountId, "challenge_id", challengeId, "purpose", purpose);
        } else {
            AuditLogs.failure("auth.code_delivery", accountId, VerificationProblem.EMAIL_DELIVERY_FAILED.message(),
                    "challenge_id", challengeId, "purpose", purpose);
        }
        try {
            challengeStore.completeDelivery(cacheKey, challengeId, failure == null);
        } catch (RuntimeException cleanupFailure) {
            // 不输出邮件正文、验证码或收件人；批次 ID 可用于关联受理与投递审计记录。
            log.error("Failed to persist email delivery result: challengeId={}, failureType={}",
                    challengeId, cleanupFailure.getClass().getName());
        }
    }

    private String buildMailText(String code, EmailCodePurpose purpose) {
        String action = switch (purpose) {
            case REGISTER -> "register a CarryPigeon account";
            case LOGIN -> "sign in to CarryPigeon";
            case CHANGE_EMAIL -> "change your CarryPigeon account email";
            case RESET_PASSWORD -> "reset your CarryPigeon account password";
        };
        return "Your verification code to " + action + " is: " + code + "\n\n"
                + "This code will expire in 10 minutes.";
    }
}
