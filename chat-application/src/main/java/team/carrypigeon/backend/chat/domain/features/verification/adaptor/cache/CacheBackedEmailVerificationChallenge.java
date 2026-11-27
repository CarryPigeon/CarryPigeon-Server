package team.carrypigeon.backend.chat.domain.features.verification.adaptor.cache;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.EmailVerificationChallenge;
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
@Slf4j
public class CacheBackedEmailVerificationChallenge implements EmailVerificationChallenge {

    // 认证凭证使用 JDK 安全随机源，不以时间戳或用户身份作为种子。
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    // 键包含用途和账号绑定，旧的通用邮箱挑战不再参与校验。
    private static final String CACHE_KEY_PREFIX = "auth:email-challenge:";
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final CodeChallengeStore challengeStore;
    private final MailSenderService mailSenderService;

    public CacheBackedEmailVerificationChallenge(
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
        CompletionStage<Void> delivery;
        try {
            var mail = new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code, purpose));
            delivery = Objects.requireNonNull(mailSenderService.send(mail), "mail sender returned no completion");
        } catch (RuntimeException exception) {
            // 同步拒绝由 HTTP 边界记录；仍须作废当前批次。
            recordDeliveryResult(cacheKey, challengeId, purpose, false);
            throw ProblemException.of(VerificationProblem.EMAIL_DELIVERY_FAILED, exception);
        }
        log.atDebug()
            .addKeyValue("challenge_id", challengeId)
            .addKeyValue("purpose", purpose)
            .log("验证码邮件已受理");
        delivery.whenComplete((_, failure) -> recordDeliveryResult(cacheKey, challengeId, purpose, failure == null));
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

    /** 按签发批次写回，以免旧回调作废重发的新码。缓存故障单独记录，保留原始投递结果。 */
    private void recordDeliveryResult(String cacheKey, String challengeId, EmailCodePurpose purpose, boolean accepted) {
        try {
            challengeStore.completeDelivery(cacheKey, challengeId, accepted);
        } catch (RuntimeException failure) {
            log.atError()
                .addKeyValue("challenge_id", challengeId)
                .addKeyValue("purpose", purpose)
                .addKeyValue("stage", "persist_result")
                .addKeyValue("failure_type", failure.getClass().getName())
                .log("验证码投递结果写回失败");
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
