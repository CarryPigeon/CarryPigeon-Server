package team.carrypigeon.backend.infrastructure.basic.exception;

/**
 * 基础设施层统一异常基类。
 * 职责：表达固定基础设施能力执行失败的统一异常语义。
 * 边界：不用于承载具体业务异常。
 */
public class InfrastructureException extends RuntimeException {

    private final InfrastructureErrorCode errorCode;

    public InfrastructureException(String message) {
        super(message);
        this.errorCode = null;
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public InfrastructureException(InfrastructureErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InfrastructureException(InfrastructureErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public InfrastructureErrorCode getErrorCode() {
        return errorCode;
    }
}
