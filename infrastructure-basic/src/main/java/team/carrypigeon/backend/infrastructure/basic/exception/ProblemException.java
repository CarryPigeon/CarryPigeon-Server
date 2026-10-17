package team.carrypigeon.backend.infrastructure.basic.exception;

import java.util.Map;

/**
 * 统一业务问题异常。
 * 职责：承载一次业务失败的全部对外信息，响应码、原因与默认消息由外部注入的 {@link ProblemMapping} 决定。
 * 边界：本类不含任何业务语义与具体错误码，也不感知任何 HTTP 类型；新增错误码不需要修改本类。
 * 说明：它属于固定基础设施机制而非业务异常，因此与 {@link InfrastructureException} 同包但语义独立，
 * 前者表达调用方可感知的业务失败，后者表达日志、配置、JSON、ID、时间等基础设施执行失败。
 */
public class ProblemException extends RuntimeException {

    private final ProblemMapping mapping;
    private final Map<String, Object> details;

    private ProblemException(ProblemMapping mapping, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.mapping = mapping;
        this.details = details;
    }

    /**
     * 使用映射自带的默认消息创建异常。
     *
     * @param mapping 外部注入的错误码
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping) {
        return new ProblemException(mapping, mapping.message(), null, null);
    }

    /**
     * 使用自定义对外消息创建异常。
     * 约束：消息必须客户端安全，不得包含内部实现细节。
     *
     * @param mapping 外部注入的错误码
     * @param message 对外消息
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping, String message) {
        return new ProblemException(mapping, message, null, null);
    }

    /**
     * 创建携带结构化明细的异常。
     *
     * @param mapping 外部注入的错误码
     * @param message 对外消息
     * @param details 可序列化明细，允许为空
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping, String message, Map<String, Object> details) {
        return new ProblemException(mapping, message, details, null);
    }

    /**
     * 创建保留根因的异常。
     * 原因：内部失败需要把原始异常留在日志链路，而对外只暴露固定消息。
     *
     * @param mapping 外部注入的错误码
     * @param message 对外消息
     * @param cause 原始异常
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping, String message, Throwable cause) {
        return new ProblemException(mapping, message, null, cause);
    }

    /**
     * 创建只携带结构化明细、使用默认消息的异常。
     *
     * @param mapping 外部注入的错误码
     * @param details 可序列化明细
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping, Map<String, Object> details) {
        return new ProblemException(mapping, mapping.message(), details, null);
    }

    /**
     * 创建使用默认消息并保留根因的异常。
     * 原因：内部失败既需要固定对外文案，又需要把原始异常留在日志链路里。
     *
     * @param mapping 外部注入的错误码
     * @param cause 原始异常
     * @return 业务问题异常
     */
    public static ProblemException of(ProblemMapping mapping, Throwable cause) {
        return new ProblemException(mapping, mapping.message(), null, cause);
    }

    /**
     * 外部注入的错误码。
     *
     * @return 错误码
     */
    public ProblemMapping mapping() {
        return mapping;
    }

    /**
     * 响应状态码，等价于 {@code mapping().status()}。
     *
     * @return 响应状态码
     */
    public int status() {
        return mapping.status();
    }

    /**
     * 对外原因，等价于 {@code mapping().reason()}。
     *
     * @return 对外原因
     */
    public String reason() {
        return mapping.reason();
    }

    /**
     * 结构化明细。
     *
     * @return 明细，缺失时为 null
     */
    public Map<String, Object> details() {
        return details;
    }
}
