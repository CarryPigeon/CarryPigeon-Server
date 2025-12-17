package team.carrypigeon.backend.api.chat.domain.node;

import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;

/**
 * 通用“查询实体”类型的 LiteFlow 节点基类。
 * <p>
 * 模板流程：
 * <ol>
 *   <li>读取查询模式（默认从 bind("key", ...) 获取）；</li>
 *   <li>根据模式从上下文取参数并执行查询；</li>
 *   <li>查询结果为 null 时调用 {@link #handleNotFound(String, CPFlowContext)} 决定行为；</li>
 *   <li>查询成功时写入 {@link #getResultKey()} 对应的上下文 key，并调用 {@link #afterSuccess(String, Object, CPFlowContext)}。</li>
 * </ol>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractSelectorNode<T> extends CPNodeComponent {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String mode = readMode(context);
        T entity = doSelect(mode, context);
        if (entity == null) {
            handleNotFound(mode, context);
            return;
        }
        context.setData(getResultKey(), entity);
        afterSuccess(mode, entity, context);
    }

    /**
     * 读取查询模式，默认从 bind("key", ...) 中获取。
     */
    protected String readMode(CPFlowContext context) throws CPReturnException {
        return requireBind(context, CPNodeBindKeys.KEY, String.class);
    }

    /**
     * 根据 mode 执行实际查询逻辑。
     *
     * @param mode    查询模式，例如 "id" / "email"
     * @param context LiteFlow 上下文
     * @return 查询到的实体，为 null 时视为“未找到”
     */
    protected abstract T doSelect(String mode, CPFlowContext context) throws Exception;

    /**
     * 查询结果写回上下文使用的 key，例如 USER_INFO / CHANNEL_INFO。
     */
    protected abstract String getResultKey();

    /**
     * 未找到数据时的处理策略。
     * 子类可以选择：
     * <ul>
     *   <li>调用 {@link #businessError(CPFlowContext, String)} 返回业务错误；</li>
     *   <li>调用 {@link #argsError(CPFlowContext)} 将其视为参数错误；</li>
     *   <li>什么都不做，表示“未找到但不视为错误”。</li>
     * </ul>
     */
    protected abstract void handleNotFound(String mode, CPFlowContext context) throws CPReturnException;

    /**
     * 查询成功后的回调，可用于补充写入上下文或记录日志。
     * 默认空实现。
     */
    protected void afterSuccess(String mode, T entity, CPFlowContext context) throws CPReturnException {
        // no-op
    }
}
