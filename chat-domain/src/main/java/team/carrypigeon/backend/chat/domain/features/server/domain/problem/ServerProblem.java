package team.carrypigeon.backend.chat.domain.features.server.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 服务端协议 feature 的错误码表。
 * 职责：声明实时通道信封与通知偏好等协议侧失败语义。
 * 边界：只服务本 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum ServerProblem implements ProblemMapping {

    /** 实时通道或通知偏好入参不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed");

    private final int status;
    private final String reason;
    private final String message;

    ServerProblem(int status, String reason, String message) {
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
