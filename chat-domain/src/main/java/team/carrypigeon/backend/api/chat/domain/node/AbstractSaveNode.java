package team.carrypigeon.backend.api.chat.domain.node;

import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 通用的“保存实体”类型 LiteFlow 节点基类。
 * <p>
 * 封装了：
 * <ul>
 *   <li>从上下文读取必填实体</li>
 *   <li>调用 DAO 执行保存</li>
 *   <li>保存失败时统一设置错误响应并中断流程</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractSaveNode<T> extends CPNodeComponent {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        T entity = requireContext(context, getContextKey(), getEntityClass());
        if (!doSave(entity)) {
            onFailure(entity, context);
            businessError(context, getErrorMessage());
        }
        afterSuccess(entity, context);
    }

    /**
     * 上下文中实体对应的 key。
     */
    protected abstract String getContextKey();

    /**
     * 实体 Class，用于类型安全的读取。
     */
    protected abstract Class<T> getEntityClass();

    /**
     * 具体保存逻辑，通常调用 DAO。
     *
     * @return true 表示保存成功，false 表示失败
     */
    protected abstract boolean doSave(T entity) throws Exception;

    /**
     * 保存失败时返回的错误文案。
     */
    protected abstract String getErrorMessage();

    /**
     * 保存成功后的回调，可用于记录日志，默认空实现。
     */
    protected void afterSuccess(T entity, CPFlowContext context) throws Exception {
        // no-op
    }

    /**
     * 保存失败（返回 false）时的回调，可用于记录日志，默认空实现。
     */
    protected void onFailure(T entity, CPFlowContext context) throws Exception {
        // no-op
    }
}
