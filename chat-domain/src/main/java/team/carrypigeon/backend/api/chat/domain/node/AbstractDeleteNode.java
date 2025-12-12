package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;

/**
 * 通用的“删除实体”类型 LiteFlow 节点基类。
 *
 * @param <T> 实体类型
 */
public abstract class AbstractDeleteNode<T> extends CPNodeComponent {

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        T entity = requireContext(context, getContextKey(), getEntityClass());
        if (!doDelete(entity)) {
            onFailure(entity, context);
            return;
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
     * 实际删除逻辑，通常调用 DAO。
     *
     * @return true 表示删除成功，false 表示失败
     */
    protected abstract boolean doDelete(T entity) throws Exception;

    /**
     * 删除失败时的错误文案。
     */
    protected abstract String getErrorMessage();

    /**
     * 删除成功后的回调，可用于记录日志，默认空实现。
     */
    protected void afterSuccess(T entity, DefaultContext context) throws Exception {
        // no-op
    }

    /**
     * 删除失败（返回 false）时的回调。默认实现输出通用错误响应，
     * 子类可覆盖以自定义错误信息或日志。
     */
    protected void onFailure(T entity, DefaultContext context) throws Exception {
        businessError(context, getErrorMessage());
    }
}
