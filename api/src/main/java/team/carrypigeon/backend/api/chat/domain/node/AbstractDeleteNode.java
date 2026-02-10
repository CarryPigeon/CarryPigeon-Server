package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 数据删除（Delete）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 从上下文读取实体对象，调用 DAO 执行删除（DELETE）操作。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 从上下文读取实体：{@link #getContextKey()}
 * 2. 执行删除：{@link #doDelete(Object)}
 * 3. 处理结果：
 *    - 成功 → {@link #afterSuccess(Object, CPFlowContext)}
 *    - 失败 → {@link #onFailure(Object, CPFlowContext)}
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("CPMessageDeleter")
 * public class CPMessageDeleterNode extends AbstractDeleteNode<CPMessage> {
 *
 *     private final MessageDao messageDao;
 *
 *     @Override
 *     protected String getContextKey() {
 *         return CPNodeMessageKeys.MESSAGE_INFO;
 *     }
 *
 *     @Override
 *     protected Class<CPMessage> getEntityClass() {
 *         return CPMessage.class;
 *     }
 *
 *     @Override
 *     protected boolean doDelete(CPMessage entity) {
 *         return messageDao.deleteById(entity.getId());
 *     }
 *
 *     @Override
 *     protected String getErrorMessage() {
 *         return "failed to delete message";
 *     }
 *
 *     @Override
 *     protected void afterSuccess(CPMessage entity, CPFlowContext context) {
 *         log.info("[MessageDeleter] 消息删除成功: mid={}", entity.getId());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractDeleteNode<T> extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractDeleteNode.class);

    /**
     * 执行删除节点主流程：读取目标、调用删除并处理结果。
     *
     * @param context 链路上下文
     * @throws Exception 删除过程中的异常
     */
    @Override
    protected final void process(CPFlowContext context) throws Exception {
        CPKey<T> key = getContextKey();
        T entity = requireContext(context, key);
        log.debug("[{}] 开始删除实体: key={}", getNodeId(), key.name());
        boolean success = doDelete(entity);
        if (!success) {
            log.error("[{}] 删除失败: key={}", getNodeId(), key.name());
            onFailure(entity, context);
            return;
        }
        log.info("[{}] 删除成功: key={}", getNodeId(), key.name());
        afterSuccess(entity, context);
    }

    /**
     * 获取实体在上下文中的 Key。
     *
     * @return 上下文 Key
     */
    protected abstract CPKey<T> getContextKey();

    /**
     * 获取实体类型。
     * <p>
     * 用于类型安全的上下文读取。
     *
     * @return 实体 Class
     */
    protected abstract Class<T> getEntityClass();

    /**
     * 执行实际删除逻辑。
     * <p>
     * 子类调用 DAO 的 delete 方法。
     *
     * @param entity 待删除的实体
     * @return true=成功, false=失败
     * @throws Exception 删除过程中的异常
     */
    protected abstract boolean doDelete(T entity) throws Exception;

    /**
     * 获取删除失败时的错误消息。
     *
     * @return 错误消息
     */
    protected abstract String getErrorMessage();

    /**
     * 删除成功后的回调。
     * <p>
     * 可用于：
     * <ul>
     *   <li>记录业务日志</li>
     *   <li>触发后续操作（如清理缓存）</li>
     * </ul>
     * 默认空实现。
     *
     * @param entity  删除的实体
     * @param context 上下文
     * @throws Exception 处理过程中的异常
     */
    protected void afterSuccess(T entity, CPFlowContext context) throws Exception {
    }

    /**
     * 删除失败时的回调。
     * <p>
     * 默认实现抛出 500 内部错误，子类可覆盖以自定义处理逻辑。
     *
     * @param entity  删除失败的实体
     * @param context 上下文
     * @throws Exception 处理过程中的异常
     */
    protected void onFailure(T entity, CPFlowContext context) throws Exception {
        fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, getErrorMessage()));
    }
}
