package team.carrypigeon.backend.api.chat.domain.error;

import lombok.Getter;

/**
 * 承载 {@link CPProblem} 的运行时异常。
 * <p>
 * 用于在 LiteFlow 节点或服务层中断链路，
 * 最终由上层异常处理器统一映射为标准错误响应。
 */
@Getter
public class CPProblemException extends RuntimeException {

    private final CPProblem problem;

    /**
     * 使用标准问题对象创建业务异常。
     *
     * @param problem 标准错误模型
     */
    public CPProblemException(CPProblem problem) {
        super(problem == null ? null : problem.message());
        this.problem = problem;
    }
}
