package team.carrypigeon.backend.chat.domain.features.verification.domain.service.verification;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.CacheBackedEmailVerificationCapability;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;

/**
 * 邮箱验证领域 API 实现。
 * 职责：收敛邮箱规范化规则，并委托 feature 内部 capability 完成签发、投递和消费。
 * 边界：不承载 auth 会话或 user 资料业务。
 */
@Service
public class EmailVerificationService implements EmailVerificationApi {

    private final CacheBackedEmailVerificationCapability cacheBackedEmailVerificationCapability;

    public EmailVerificationService(CacheBackedEmailVerificationCapability cacheBackedEmailVerificationCapability) {
        this.cacheBackedEmailVerificationCapability = cacheBackedEmailVerificationCapability;
    }

    @Override
    public void issueCode(String email) {
        try {
            cacheBackedEmailVerificationCapability.issueCode(email);
            AuditLogs.success("auth.code_issue", null);
        } catch (ProblemException exception) {
            AuditLogs.failure("auth.code_issue", null, exception.reason());
            throw exception;
        }
    }

    @Override
    public void verifyCode(String email,String code) {
        cacheBackedEmailVerificationCapability.verifyCode(email, code);
    }
}
