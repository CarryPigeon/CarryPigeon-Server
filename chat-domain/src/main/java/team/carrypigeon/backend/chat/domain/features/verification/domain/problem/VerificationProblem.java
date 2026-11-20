package team.carrypigeon.backend.chat.domain.features.verification.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 验证码 feature 的错误码表。
 * 职责：完整声明验证码校验与投递链路上的失败语义。
 * 边界：只服务验证码 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum VerificationProblem implements ProblemMapping {

    /** 验证码入参或验证码本身不合法。 */
    INVALID_CODE(12001, 422, "validation_failed", "validation failed"),
    /**邮箱不合法*/
    INVALID_EMAIL(12002, 422, "invalid_email", "invalid email"),
    /** 验证码用途与账号绑定不符合签发或消费契约。 */
    INVALID_PURPOSE(12003, 422, "validation_failed", "invalid email code purpose or account binding"),
    /** 修改邮箱挑战只能由已认证账号签发和消费。 */
    AUTHENTICATION_REQUIRED(11001, 401, "authentication_required", "authentication is required to change email"),
    /** 缓存服务未就绪，无法签发或校验验证码。 */
    CACHE_UNAVAILABLE(10011, 503, "cache_service_unavailable", "cache service is unavailable"),

    /** 邮件服务未就绪。 */
    MAIL_UNAVAILABLE(10011, 503, "mail_service_unavailable", "mail service is unavailable"),
    /**验证码尝试次数过多*/
    TOO_MANY_REQUESTS(12004, 429, "too_many_requests", "too many requests, please retry later"),
    /** 验证码邮件投递失败。 */
    EMAIL_DELIVERY_FAILED(12005, 503, "email_delivery_failed", "failed to deliver verification email");

    private final int code;
    private final int status;
    private final String reason;
    private final String message;

    VerificationProblem(int code, int status, String reason, String message) {
        this.code = code;
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
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
