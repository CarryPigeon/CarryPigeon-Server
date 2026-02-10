package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 数据查询（Selector）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 根据查询模式从数据库查询实体，并将结果写入上下文。
 * 支持多种查询模式（如按 ID、按 email 等），通过 bind 参数配置。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 读取查询模式：{@link #readMode(CPFlowContext)}
 * 2. 执行查询：{@link #doSelect(String, CPFlowContext)}
 * 3. 处理结果：
 *    - null → {@link #handleNotFound(String, CPFlowContext)}
 *    - 非 null → 写入 {@link #getResultKey()}，调用 {@link #afterSuccess(String, Object, CPFlowContext)}
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("CPUserSelector")
 * public class CPUserSelectorNode extends AbstractSelectorNode<CPUser> {
 *
 *     private final UserDao userDao;
 *
 *     @Override
 *     protected CPUser doSelect(String mode, CPFlowContext context) {
 *         return switch (mode) {
 *             case "id" -> {
 *                 Long id = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
 *                 yield userDao.getById(id);
 *             }
 *             case "email" -> {
 *                 String email = requireContext(context, CPNodeUserKeys.USER_INFO_EMAIL);
 *                 yield userDao.getByEmail(email);
 *             }
 *             default -> throw new IllegalArgumentException("unsupported mode: " + mode);
 *         };
 *     }
 *
 *     @Override
 *     protected CPKey<CPUser> getResultKey() {
 *         return CPNodeUserKeys.USER_INFO;
 *     }
 *
 *     @Override
 *     protected void handleNotFound(String mode, CPFlowContext context) {
 *         notFound("user not found");
 *     }
 * }
 * }</pre>
 *
 * <h2>XML 配置示例</h2>
 * <pre>{@code
 * <!-- 按 ID 查询 -->
 * CPUserSelector.bind("key","id")
 *
 * <!-- 按 email 查询 -->
 * CPUserSelector.bind("key","email")
 * }</pre>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractSelectorNode<T> extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractSelectorNode.class);

    /**
     * 执行查询节点主流程：解析模式、查询实体并回写上下文。
     *
     * @param context 链路上下文
     * @throws Exception 查询过程中的异常
     */
    @Override
    protected final void process(CPFlowContext context) throws Exception {
        String mode = readMode(context);
        log.debug("[{}] 开始查询: mode={}", getNodeId(), mode);
        T entity = doSelect(mode, context);
        if (entity == null) {
            log.debug("[{}] 查询结果为空: mode={}", getNodeId(), mode);
            handleNotFound(mode, context);
            return;
        }
        CPKey<T> resultKey = getResultKey();
        context.set(resultKey, entity);
        log.debug("[{}] 查询成功: mode={}, resultKey={}", getNodeId(), mode, resultKey.name());
        afterSuccess(mode, entity, context);
    }

    /**
     * 读取查询模式。
     * <p>
     * 默认从 bind 参数 "key" 获取，子类可覆盖以实现自定义逻辑。
     *
     * @param context 上下文
     * @return 查询模式字符串
     */
    protected String readMode(CPFlowContext context) {
        return requireBind(CPNodeBindKeys.KEY, String.class);
    }

    /**
     * 执行实际查询逻辑。
     * <p>
     * 子类根据 mode 从上下文读取查询参数，调用 DAO 执行查询。
     *
     * <h3>实现规范</h3>
     * <ul>
     *   <li>使用 {@link #requireContext} 读取查询参数</li>
     *   <li>使用 {@link #select} 包装查询以启用缓存</li>
     *   <li>返回 null 表示未找到</li>
     * </ul>
     *
     * @param mode    查询模式（如 "id"、"email"）
     * @param context 上下文
     * @return 查询结果，null 表示未找到
     * @throws Exception 查询过程中的异常
     */
    protected abstract T doSelect(String mode, CPFlowContext context) throws Exception;

    /**
     * 获取结果写入上下文的 Key。
     * <p>
     * 子类必须返回对应业务域的 Key 常量。
     *
     * @return 上下文 Key
     */
    protected abstract CPKey<T> getResultKey();

    /**
     * 处理未找到数据的情况。
     * <p>
     * 子类根据业务需求选择处理方式：
     * <ul>
     *   <li>{@link #notFound(String)}：返回 404 错误</li>
     *   <li>{@link #forbidden(String, String)}：返回 403 错误（如非成员校验）</li>
     *   <li>空实现：允许 null 结果继续流程</li>
     * </ul>
     *
     * @param mode    查询模式
     * @param context 上下文
     */
    protected abstract void handleNotFound(String mode, CPFlowContext context);

    /**
     * 查询成功后的回调。
     * <p>
     * 可用于：
     * <ul>
     *   <li>写入额外的上下文数据</li>
     *   <li>记录业务日志</li>
     * </ul>
     * 默认空实现。
     *
     * @param mode    查询模式
     * @param entity  查询结果
     * @param context 上下文
     */
    protected void afterSuccess(String mode, T entity, CPFlowContext context) {
    }
}
