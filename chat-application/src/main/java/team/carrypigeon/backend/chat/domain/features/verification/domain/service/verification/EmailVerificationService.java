package team.carrypigeon.backend.chat.domain.features.verification.domain.service.verification;

import java.util.Locale;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.EmailVerificationChallenge;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 邮箱验证领域 API 实现。
 * 职责：收敛邮箱规范化规则，并委托 feature 内部 capability 完成签发、投递和消费。
 * 边界：不承载 auth 会话或 user 资料业务。
 */
@Service
public class EmailVerificationService implements EmailVerificationApi {

    private final EmailVerificationChallenge emailVerificationChallenge;

    public EmailVerificationService(EmailVerificationChallenge emailVerificationChallenge) {
        this.emailVerificationChallenge = emailVerificationChallenge;
    }

    @Override
    public void issueCode(String email, EmailCodePurpose purpose, Long accountId) {
        validateChallenge(email, purpose, accountId);
        emailVerificationChallenge.issueCode(email.trim().toLowerCase(Locale.ROOT), purpose, accountId);
    }

    @Override
    public void verifyCode(String email, String code, EmailCodePurpose purpose, Long accountId) {
        validateChallenge(email, purpose, accountId);
        emailVerificationChallenge.verifyCode(email.trim().toLowerCase(Locale.ROOT), code, purpose, accountId);
    }

    /** 签发与消费使用相同的身份约束，防止缺少账号绑定的修改邮箱挑战进入缓存。 */
    private void validateChallenge(String email, EmailCodePurpose purpose, Long accountId) {
        if (email == null || email.isBlank()) {
            throw ProblemException.of(VerificationProblem.INVALID_EMAIL);
        }
        if (purpose == null || (purpose != EmailCodePurpose.CHANGE_EMAIL && accountId != null)) {
            throw ProblemException.of(VerificationProblem.INVALID_PURPOSE);
        }
        if (purpose == EmailCodePurpose.CHANGE_EMAIL && accountId == null) {
            throw ProblemException.of(VerificationProblem.AUTHENTICATION_REQUIRED);
        }
    }
}
