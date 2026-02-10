package team.carrypigeon.backend.api.chat.domain.error;

/**
 * API 统一错误对象。
 *
 * @param status HTTP 语义状态码。
 * @param reason 机器可读错误原因。
 * @param message 人类可读错误信息。
 * @param details 扩展错误细节。
 */
public record CPProblem(int status,
                        CPProblemReason reason,
                        String message,
                        Object details) {

    /**
     * 规范化错误原因。
     */
    public CPProblem {
        if (reason == null) {
            reason = CPProblemReason.INTERNAL_ERROR;
        }
    }

    /**
     * 基于标准原因创建错误。
     *
     * @param reason 标准错误原因。
     * @param message 错误描述。
     * @return 统一错误对象。
     */
    public static CPProblem of(CPProblemReason reason, String message) {
        CPProblemReason safeReason = reason != null ? reason : CPProblemReason.INTERNAL_ERROR;
        return new CPProblem(safeReason.status(), safeReason, message, null);
    }

    /**
     * 基于标准原因创建错误（携带细节）。
     *
     * @param reason 标准错误原因。
     * @param message 错误描述。
     * @param details 扩展错误细节。
     * @return 统一错误对象。
     */
    public static CPProblem of(CPProblemReason reason, String message, Object details) {
        CPProblemReason safeReason = reason != null ? reason : CPProblemReason.INTERNAL_ERROR;
        return new CPProblem(safeReason.status(), safeReason, message, details);
    }

    /**
     * 通过状态码与字符串原因创建错误。
     *
     * @param status HTTP 状态码。
     * @param reason 原因代码字符串。
     * @param message 错误描述。
     * @return 统一错误对象。
     * @deprecated 建议改用 {@link #of(CPProblemReason, String)}。
     */
    @Deprecated
    public static CPProblem of(int status, String reason, String message) {
        return new CPProblem(status, CPProblemReason.fromCode(reason), message, null);
    }

    /**
     * 通过状态码与字符串原因创建错误（携带细节）。
     *
     * @param status HTTP 状态码。
     * @param reason 原因代码字符串。
     * @param message 错误描述。
     * @param details 扩展错误细节。
     * @return 统一错误对象。
     * @deprecated 建议改用 {@link #of(CPProblemReason, String, Object)}。
     */
    @Deprecated
    public static CPProblem of(int status, String reason, String message, Object details) {
        return new CPProblem(status, CPProblemReason.fromCode(reason), message, details);
    }
}
