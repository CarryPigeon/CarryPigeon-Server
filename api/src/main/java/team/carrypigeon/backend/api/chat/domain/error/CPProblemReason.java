package team.carrypigeon.backend.api.chat.domain.error;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * API 错误原因枚举（系统唯一 reason 字典）。
 * <p>
 * 每个枚举项都绑定：
 * <ul>
 *   <li>机器可读 reason code（对外协议字段）</li>
 *   <li>默认 HTTP status（传输层状态码）</li>
 * </ul>
 * <p>
 * 使用约束：
 * <ul>
 *   <li>控制层、过滤器、WS 处理器都应复用本枚举输出 reason</li>
 *   <li>新增错误场景优先补枚举，避免散落硬编码字符串</li>
 * </ul>
 */
public enum CPProblemReason {
    UNAUTHORIZED("unauthorized", 401),
    TOKEN_EXPIRED("token_expired", 401),

    API_VERSION_UNSUPPORTED("api_version_unsupported", 406),

    FORBIDDEN("forbidden", 403),
    NOT_CHANNEL_MEMBER("not_channel_member", 403),
    NOT_CHANNEL_ADMIN("not_channel_admin", 403),
    NOT_CHANNEL_OWNER("not_channel_owner", 403),
    USER_MUTED("user_muted", 403),
    CANNOT_BAN_ADMIN("cannot_ban_admin", 403),
    CANNOT_CHANGE_OWNER_AUTHORITY("cannot_change_owner_authority", 403),

    NOT_FOUND("not_found", 404),

    CONFLICT("conflict", 409),
    ALREADY_IN_CHANNEL("already_in_channel", 409),
    APPLICATION_ALREADY_PROCESSED("application_already_processed", 409),
    IDEMPOTENCY_PROCESSING("idempotency_processing", 409),

    REQUIRED_PLUGIN_MISSING("required_plugin_missing", 412),

    VALIDATION_FAILED("validation_failed", 422),
    SCHEMA_INVALID("schema_invalid", 422),
    CURSOR_INVALID("cursor_invalid", 422),
    EVENT_TOO_OLD("event_too_old", 422),
    CHANNEL_FIXED("channel_fixed", 422),
    EMAIL_INVALID("email_invalid", 422),
    EMAIL_CODE_INVALID("email_code_invalid", 422),

    RATE_LIMITED("rate_limited", 429),

    INTERNAL_ERROR("internal_error", 500),
    EMAIL_SERVICE_DISABLED("email_service_disabled", 500),
    EMAIL_SEND_FAILED("email_send_failed", 500),
    EMAIL_EXISTS("email_exists", 409);

    private static final Map<String, CPProblemReason> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CPProblemReason::code, Function.identity()));

    private final String code;
    private final int status;

    /**
     * 构造错误原因枚举。
     *
     *  code 协议层错误代码。
     *  status 默认 HTTP 状态码。
     */
    CPProblemReason(String code, int status) {
        this.code = code;
        this.status = status;
    }

    /**
     * 返回协议层 reason code。
     *
     * @return reason code
     */
    public String code() {
        return code;
    }

    /**
     * 返回该 reason 对应的默认 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int status() {
        return status;
    }

    /**
     * 通过协议 reason code 反查枚举。
     *
     * @param code 协议 code
     * @return 对应枚举；当 code 为空或未知时返回 INTERNAL_ERROR
     */
    public static CPProblemReason fromCode(String code) {
        if (code == null || code.isBlank()) {
            return INTERNAL_ERROR;
        }
        return BY_CODE.getOrDefault(code, INTERNAL_ERROR);
    }
}
