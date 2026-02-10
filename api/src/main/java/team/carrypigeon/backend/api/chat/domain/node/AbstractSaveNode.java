package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 数据保存（Save）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 从上下文读取实体对象，调用 DAO 执行保存（INSERT）操作。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 从上下文读取实体：{@link #getContextKey()}
 * 2. 执行保存：{@link #doSave(Object)}
 * 3. 处理结果：
 *    - 成功 → {@link #afterSuccess(Object, CPFlowContext)}
 *    - 失败 → {@link #onFailure(Object, CPFlowContext)}，抛出 500 错误
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("CPMessageSaver")
 * public class CPMessageSaverNode extends AbstractSaveNode<CPMessage> {
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
 *     protected boolean doSave(CPMessage entity) {
 *         return messageDao.insert(entity);
 *     }
 *
 *     @Override
 *     protected String getErrorMessage() {
 *         return "failed to save message";
 *     }
 *
 *     @Override
 *     protected void afterSuccess(CPMessage entity, CPFlowContext context) {
 *         log.info("[MessageSaver] 消息保存成功: mid={}, cid={}", entity.getId(), entity.getCid());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractSaveNode<T> extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractSaveNode.class);

    /**
     * 执行保存节点主流程：读取实体、落库并处理成败分支。
     *
     * @param context 链路上下文
     * @throws Exception 保存过程中的异常
     */
    @Override
    protected final void process(CPFlowContext context) throws Exception {
        CPKey<T> key = getContextKey();
        T entity = requireContext(context, key);
        log.debug("[{}] 开始保存实体: key={}", getNodeId(), key.name());
        boolean success = doSave(entity);
        if (!success) {
            log.error("[{}] 保存失败: key={}", getNodeId(), key.name());
            onFailure(entity, context);
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, getErrorMessage()));
            return;
        }
        log.info("[{}] 保存成功: key={}", getNodeId(), key.name());
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
     * 执行实际保存逻辑。
     * <p>
     * 子类调用 DAO 的 insert 方法。
     *
     * @param entity 待保存的实体
     * @return true=成功, false=失败
     * @throws Exception 保存过程中的异常
     */
    protected abstract boolean doSave(T entity) throws Exception;

    /**
     * 获取保存失败时的错误消息。
     *
     * @return 错误消息
     */
    protected abstract String getErrorMessage();

    /**
     * 保存成功后的回调。
     * <p>
     * 可用于：
     * <ul>
     *   <li>记录业务日志</li>
     *   <li>写入额外的上下文数据</li>
     * </ul>
     * 默认空实现。
     *
     * @param entity  保存的实体
     * @param context 上下文
     * @throws Exception 处理过程中的异常
     */
    protected void afterSuccess(T entity, CPFlowContext context) throws Exception {
    }

    /**
     * 保存失败时的回调。
     * <p>
     * 可用于记录错误日志或执行补偿操作。
     * 默认空实现。
     *
     * @param entity  保存失败的实体
     * @param context 上下文
     * @throws Exception 处理过程中的异常
     */
    protected void onFailure(T entity, CPFlowContext context) throws Exception {
    }
}
