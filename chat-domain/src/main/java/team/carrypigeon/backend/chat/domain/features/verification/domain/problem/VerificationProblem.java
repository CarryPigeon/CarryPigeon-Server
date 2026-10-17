package team.carrypigeon.backend.chat.domain.features.verification.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 验证码 feature 的错误码表。
 * 职责：完整声明验证码校验与投递链路上的失败语义。
 * 边界：只服务验证码 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum VerificationProblem implements ProblemMapping {

    /** 验证码入参或验证码本身不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed"),

    /** 邮件服务未就绪。 */
    MAIL_UNAVAILABLE(503, "mail_service_unavailable", "mail service is unavailable"),

    /** 验证码邮件投递失败。 */
    EMAIL_DELIVERY_FAILED(503, "email_delivery_failed", "failed to deliver verification email");

    private final int status;
    private final String reason;
    private final String message;

    VerificationProblem(int status, String reason, String message) {
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public String message() {
        return message;
    }
}
