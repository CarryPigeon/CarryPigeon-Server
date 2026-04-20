package team.carrypigeon.backend.chat.domain.shared.domain.problem;

/**
 * 统一业务问题异常。
 * 职责：表达业务入口可识别的问题中断语义，并由统一异常处理转换成稳定响应。
 * 边界：不直接暴露基础设施细节，不替代基础设施异常。
 */
public class ProblemException extends RuntimeException {

    private final ProblemType type;
    private final String reason;

    private ProblemException(ProblemType type, String reason, String message) {
        super(message);
        this.type = type;
        this.reason = reason;
    }

    public static ProblemException validationFailed(String message) {
        return new ProblemException(ProblemType.VALIDATION, "validation_failed", message);
    }

    public static ProblemException forbidden(String reason, String message) {
        return new ProblemException(ProblemType.FORBIDDEN, reason, message);
    }

    public static ProblemException notFound(String message) {
        return new ProblemException(ProblemType.NOT_FOUND, "not_found", message);
    }

    public static ProblemException fail(String reason, String message) {
        return new ProblemException(ProblemType.INTERNAL, reason, message);
    }

    public ProblemType type() {
        return type;
    }

    public String reason() {
        return reason;
    }
}
