package team.carrypigeon.backend.chat.domain.features.user.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 用户资料 feature 的错误码表。
 * 职责：声明用户资料查询与维护相关的失败语义。
 * 边界：只服务用户资料 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum UserProblem implements ProblemMapping {

    /** 目标用户资料不存在。 */
    PROFILE_NOT_FOUND(404, "not_found", "user profile does not exist");

    private final int status;
    private final String reason;
    private final String message;

    UserProblem(int status, String reason, String message) {
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
