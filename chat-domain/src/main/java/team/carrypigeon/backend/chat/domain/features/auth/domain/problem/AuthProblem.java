package team.carrypigeon.backend.chat.domain.features.auth.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 认证 feature 的错误码表。
 * 职责：完整声明认证自身需要的失败语义，包括入参校验、凭证、令牌签发与账号查询。
 * 边界：只服务认证 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum AuthProblem implements ProblemMapping {

    /** 认证相关入参不合法。 */
    INVALID_INPUT(10001, 422, "validation_failed", "validation failed"),

    /** 未提供认证凭证。 */
    AUTHENTICATION_REQUIRED(11001, 401, "unauthorized", "authentication is required"),

    /** access token 无法验证。 */
    INVALID_ACCESS_TOKEN(11003, 401, "unauthorized", "access token is invalid"),

    /** refresh token 无法验证。 */
    INVALID_REFRESH_TOKEN(11004, 401, "unauthorized", "refresh token is invalid"),

    /** 令牌格式或载荷非法。 */
    INVALID_TOKEN(11005, 401, "unauthorized", "token is invalid"),

    /** 账号或密码不匹配。 */
    INVALID_CREDENTIALS(11006, 401, "unauthorized", "username or password is invalid"),

    /** 令牌已过期。 */
    TOKEN_EXPIRED(11002, 401, "token_expired", "token is expired"),

    /** 部署侧关闭了密码登录。 */
    PASSWORD_LOGIN_DISABLED(11007, 403, "password_login_disabled", "password login is disabled"),

    /** 密码登录短时配额已耗尽，重试时间由异常明细提供。 */
    LOGIN_RATE_LIMITED(11008, 429, "too_many_requests", "too many login attempts; retry later"),

    /** 共享限流状态不可用时拒绝密码登录，不降级为无限尝试。 */
    LOGIN_LIMIT_UNAVAILABLE(10011, 503, "cache_service_unavailable", "login rate limiter is unavailable"),

    /** 目标认证账号不存在。 */
    ACCOUNT_NOT_FOUND(11009, 404, "not_found", "auth account does not exist"),

    /** 注册时用户名已被占用。 */
    USERNAME_ALREADY_EXISTS(11010, 409, "username_already_exists", "username already exists"),

    /** 注册或改绑时邮箱已被其它账号占用。 */
    EMAIL_ALREADY_EXISTS(11011, 409, "email_already_exists", "email already exists"),

    /** 令牌摘要计算失败。 */
    TOKEN_HASH_FAILED(10012, 500, "internal_error", "internal server error"),

    /** 令牌签名失败。 */
    TOKEN_SIGN_FAILED(10012, 500, "internal_error", "internal server error");

    private final int code;
    private final int status;
    private final String reason;
    private final String message;

    AuthProblem(int code, int status, String reason, String message) {
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
