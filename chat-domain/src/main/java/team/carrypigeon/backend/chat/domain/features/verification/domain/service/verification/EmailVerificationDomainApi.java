package team.carrypigeon.backend.chat.domain.features.verification.domain.service.verification;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.EmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 邮箱验证领域 API 实现。
 * 职责：收敛邮箱规范化规则，并委托 feature 内部 capability 完成签发、投递和消费。
 * 边界：不承载 auth 会话或 user 资料业务。
 */
@Service
public class EmailVerificationDomainApi implements EmailVerificationApi {

    private final EmailVerificationCapability emailVerificationCapability;

    public EmailVerificationDomainApi(EmailVerificationCapability emailVerificationCapability) {
        this.emailVerificationCapability = emailVerificationCapability;
    }

    @Override
    public void issueCode(String email) {
        emailVerificationCapability.issueCode(email);
    }

    @Override
    public void verifyCode(String email,String code) {
        emailVerificationCapability.verifyCode(email, code);
    }
}
