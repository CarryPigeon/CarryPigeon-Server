package team.carrypigeon.backend.chat.domain.shared.domain.problem;

import java.util.Map;

/**
 * 统一业务问题异常。
 * 职责：表达业务入口可识别的问题中断语义，并由统一异常处理转换成稳定响应。
 * 边界：不直接暴露基础设施细节，不替代基础设施异常。
 */
public class ProblemException extends RuntimeException {

    private final ProblemType type;
    private final String reason;
    private final Map<String, Object> details;

    private ProblemException(ProblemType type, String reason, String message, Map<String, Object> details) {
        super(message);
        this.type = type;
        this.reason = reason;
        this.details = details;
    }

    /**
     * 创建默认 reason 的校验失败异常。
     * 原因：用于不需要细分校验 `reason` 的常规参数错误场景。
     */
    public static ProblemException validationFailed(String message) {
        return new ProblemException(ProblemType.VALIDATION, "validation_failed", message, null);
    }

    /**
     * 创建带自定义 reason 的校验失败异常。
     * 输入：稳定可检索的 reason 与对外错误消息。
     */
    public static ProblemException validationFailed(String reason, String message) {
        return new ProblemException(ProblemType.VALIDATION, reason, message, null);
    }

    /**
     * 创建带附加详情的校验失败异常。
     * 输出：异常会携带可序列化 details 供统一异常处理下发。
     */
    public static ProblemException validationFailed(String reason, String message, Map<String, Object> details) {
        return new ProblemException(ProblemType.VALIDATION, reason, message, details);
    }

    /**
     * 创建权限不足异常。
     */
    public static ProblemException forbidden(String reason, String message) {
        return new ProblemException(ProblemType.FORBIDDEN, reason, message, null);
    }

    /**
     * 创建默认 reason 的冲突异常。
     */
    public static ProblemException conflict(String message) {
        return new ProblemException(ProblemType.CONFLICT, "conflict", message, null);
    }

    /**
     * 创建带自定义 reason 的冲突异常。
     */
    public static ProblemException conflict(String reason, String message) {
        return new ProblemException(ProblemType.CONFLICT, reason, message, null);
    }

    /**
     * 创建资源不存在异常。
     */
    public static ProblemException notFound(String message) {
        return new ProblemException(ProblemType.NOT_FOUND, "not_found", message, null);
    }

    /**
     * 创建内部失败异常。
     * 边界：用于业务层显式收口的失败语义，不替代基础设施异常。
     */
    public static ProblemException fail(String reason, String message) {
        return new ProblemException(ProblemType.INTERNAL, reason, message, null);
    }

    public ProblemType type() {
        return type;
    }

    public String reason() {
        return reason;
    }

    public Map<String, Object> details() {
        return details;
    }
}
