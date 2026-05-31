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

    public static ProblemException validationFailed(String message) {
        return new ProblemException(ProblemType.VALIDATION, "validation_failed", message, null);
    }

    public static ProblemException validationFailed(String reason, String message) {
        return new ProblemException(ProblemType.VALIDATION, reason, message, null);
    }

    public static ProblemException validationFailed(String reason, String message, Map<String, Object> details) {
        return new ProblemException(ProblemType.VALIDATION, reason, message, details);
    }

    public static ProblemException forbidden(String reason, String message) {
        return new ProblemException(ProblemType.FORBIDDEN, reason, message, null);
    }

    public static ProblemException conflict(String message) {
        return new ProblemException(ProblemType.CONFLICT, "conflict", message, null);
    }

    public static ProblemException conflict(String reason, String message) {
        return new ProblemException(ProblemType.CONFLICT, reason, message, null);
    }

    public static ProblemException notFound(String message) {
        return new ProblemException(ProblemType.NOT_FOUND, "not_found", message, null);
    }

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
