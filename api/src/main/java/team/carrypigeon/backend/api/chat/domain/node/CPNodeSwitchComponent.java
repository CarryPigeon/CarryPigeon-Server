package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * LiteFlow 分支（Switch）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 在 LiteFlow SWITCH 语句中，根据条件返回分支标记，决定后续执行哪个分支。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("ModeSwitch")
 * public class ModeSwitchNode extends CPNodeSwitchComponent {
 *
 *     @Override
 *     protected String process(CPFlowContext context) {
 *         String mode = requireBind(CPNodeBindKeys.KEY, String.class);
 *         return switch (mode) {
 *             case "create" -> "createBranch";
 *             case "update" -> "updateBranch";
 *             default -> "defaultBranch";
 *         };
 *     }
 * }
 * }</pre>
 *
 * <h2>XML 配置示例</h2>
 * <pre>{@code
 * <chain name="example">
 *     SWITCH(ModeSwitch).to(
 *         createBranch.id("createBranch"),
 *         updateBranch.id("updateBranch"),
 *         defaultBranch.id("defaultBranch")
 *     )
 * </chain>
 * }</pre>
 *
 * @see CPFlowContext
 */
public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeSwitchComponent.class);

    /**
     * LiteFlow 入口方法（框架调用）。
     * <p>
     * 从 LiteFlow Slot 获取 {@link CPFlowContext} 并委托给 {@link #process(CPFlowContext)}。
     *
     * @return 分支标记
     * @throws IllegalStateException 如果上下文不存在
     */
    @Override
    public final String processSwitch() throws Exception {
        CPFlowContext context = this.getContextBean(CPFlowContext.class);
        if (context == null) {
            log.error("[{}] CPFlowContext 不存在，无法执行分支节点", getNodeId());
            throw new IllegalStateException("CPFlowContext not found in LiteFlow slot, node=" + getNodeId());
        }
        String branch = process(context);
        log.debug("[{}] 分支选择: branch={}", getNodeId(), branch);
        return branch;
    }

    /**
     * 分支选择逻辑。
     * <p>
     * 子类实现具体的分支判断逻辑，返回分支标记。
     *
     * @param context LiteFlow 上下文
     * @return 分支标记（对应 XML 中 {@code .id("xxx")} 的值）
     * @throws Exception 处理过程中的异常
     */
    protected abstract String process(CPFlowContext context) throws Exception;

    // ==================== 工具方法 ====================

    /**
     * 参数校验失败的快捷方法。
     *
     * @throws CPProblemException 总是抛出
     */
    protected void validationFailed() {
        fail(CPProblem.of(422, "validation_failed", "validation failed"));
    }

    /**
     * 参数校验失败（带详情）的快捷方法。
     *
     * @param message 错误描述
     * @throws CPProblemException 总是抛出
     */
    protected void validationFailed(String message) {
        fail(CPProblem.of(422, "validation_failed", message));
    }

    /**
     * 从上下文读取必填数据。
     *
     * @param context 上下文
     * @param key     数据 Key
     * @param type    数据类型
     * @param <T>     返回类型
     * @return 数据值（非 null）
     * @throws CPProblemException 如果数据不存在
     */
    protected <T> T requireContext(CPFlowContext context, String key, Class<T> type) {
        Object raw = context.getData(key);
        if (raw == null) {
            log.error("[{}] 必填上下文参数缺失: key={}, type={}",
                    getNodeId(), key, type != null ? type.getSimpleName() : "null");
            throw new CPProblemException(
                    CPProblem.of(422, "validation_failed", "missing required parameter: " + key)
            );
        }
        if (type != null && !type.isInstance(raw)) {
            log.error("[{}] 上下文参数类型不匹配: key={}, expected={}, actual={}",
                    getNodeId(), key, type.getSimpleName(), raw.getClass().getSimpleName());
            throw new CPProblemException(
                    CPProblem.of(500, "internal_error", "context type mismatch: " + key)
            );
        }
        //noinspection unchecked
        return type != null ? type.cast(raw) : (T) raw;
    }

    /**
     * 从上下文读取必填数据（强类型 Key）。
     *
     * @param context 上下文
     * @param key     强类型 Key
     * @param <T>     返回类型
     * @return 数据值（非 null）
     * @throws CPProblemException 如果数据不存在或类型不匹配
     */
    protected <T> T requireContext(CPFlowContext context, CPKey<T> key) {
        if (context == null) {
            throw new CPProblemException(CPProblem.of(500, "internal_error", "context is null"));
        }
        if (key == null) {
            throw new CPProblemException(CPProblem.of(500, "internal_error", "key is null"));
        }
        T value;
        try {
            value = context.get(key);
        } catch (IllegalStateException ex) {
            log.error("[{}] 上下文参数类型不匹配: key={}", getNodeId(), key.name(), ex);
            throw new CPProblemException(CPProblem.of(500, "internal_error", "context type mismatch: " + key.name()));
        }
        if (value == null) {
            log.error("[{}] 必填上下文参数缺失: key={}, type={}",
                    getNodeId(), key.name(), key.type().getSimpleName());
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "missing required parameter: " + key.name()));
        }
        return value;
    }

    /**
     * 从 LiteFlow bind 参数读取必填配置。
     *
     * @param key  参数名
     * @param type 参数类型
     * @param <T>  返回类型
     * @return 参数值（非 null）
     * @throws CPProblemException 如果参数不存在
     */
    protected <T> T requireBind(String key, Class<T> type) {
        T value = getBindData(key, type);
        if (value == null) {
            log.error("[{}] 必填 bind 参数缺失: key={}, type={}",
                    getNodeId(), key, type != null ? type.getSimpleName() : "null");
            throw new CPProblemException(
                    CPProblem.of(500, "internal_error", "missing bind parameter: " + key)
            );
        }
        return value;
    }

    /**
     * 抛出标准化业务错误。
     *
     * @param problem 错误信息
     * @throws CPProblemException 总是抛出
     */
    protected void fail(CPProblem problem) {
        CPProblem safe = problem != null ? problem
                : CPProblem.of(500, "internal_error", "internal error");
        log.debug("[{}] 业务失败: status={}, reason={}, message={}",
                getNodeId(), safe.status(), safe.reason(), safe.message());
        throw new CPProblemException(safe);
    }
}
