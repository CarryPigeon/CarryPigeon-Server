package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * 请求参数绑定（Bind）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 将 HTTP 请求 DTO 中的参数解析并绑定到 LiteFlow 上下文的业务 Key 中，
 * 使后续业务节点可以通过标准 Key 读取参数。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 从 {@link CPFlowKeys#REQUEST} 读取请求 DTO
 * 2. 调用 {@link #validate(Object, CPFlowContext)} 校验参数
 * 3. 调用 {@link #bind(Object, CPFlowContext)} 绑定参数到上下文
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("ApiMessageListBind")
 * public class ApiMessageListBindNode extends AbstractBindNode<MessageListRequest> {
 *
 *     public ApiMessageListBindNode() {
 *         super(MessageListRequest.class);
 *     }
 *
 *     @Override
 *     protected void bind(MessageListRequest request, CPFlowContext context) {
 *         context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, request.cid());
 *         context.set(CPNodeMessageKeys.MESSAGE_LIST_COUNT, request.limit());
 *     }
 * }
 * }</pre>
 *
 * @param <R> 请求 DTO 类型
 * @see CPFlowKeys#REQUEST
 */
public abstract class AbstractBindNode<R> extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractBindNode.class);

    private final Class<R> requestType;

    /**
     * 构造绑定节点。
     *
     * @param requestType 请求 DTO 类型
     */
    protected AbstractBindNode(Class<R> requestType) {
        this.requestType = requestType;
    }

    /**
     * 执行绑定节点主流程：读取输入、构造输出并写入上下文。
     *
     * @param context 链路上下文
     * @throws Exception 绑定过程中的异常
     */
    @Override
    protected final void process(CPFlowContext context) throws Exception {
        Object raw = context.get(CPFlowKeys.REQUEST);
        if (raw == null) {
            log.error("[{}] 请求 DTO 不存在: key={}", getNodeId(), CPFlowKeys.REQUEST.name());
            validationFailed("request body is required");
            return;
        }
        if (!requestType.isInstance(raw)) {
            log.error("[{}] 请求 DTO 类型不匹配: expected={}, actual={}",
                    getNodeId(), requestType.getSimpleName(), raw.getClass().getSimpleName());
            validationFailed("invalid request type");
            return;
        }

        @SuppressWarnings("unchecked")
        R request = (R) raw;
        validate(request, context);
        bind(request, context);

        log.debug("[{}] 参数绑定完成: requestType={}", getNodeId(), requestType.getSimpleName());
    }

    /**
     * 校验请求参数。
     * <p>
     * 默认空实现，子类可覆盖以添加校验逻辑。
     * 校验失败时应调用 {@link #validationFailed(String)} 抛出异常。
     *
     * <h3>示例</h3>
     * <pre>{@code
     * @Override
     * protected void validate(MessageListRequest request, CPFlowContext context) {
     *     if (request.limit() != null && request.limit() > 100) {
     *         validationFailed("limit cannot exceed 100");
     *     }
     * }
     * }</pre>
     *
     * @param request 请求 DTO
     * @param context 上下文（用于访问其他数据）
     */
    protected void validate(R request, CPFlowContext context) {
    }

    /**
     * 将请求参数绑定到上下文。
     * <p>
     * 子类必须实现此方法，将请求 DTO 中的字段写入上下文的业务 Key。
     *
     * <h3>命名规范</h3>
     * <ul>
     *   <li>使用 {@code CPNode*Keys} 中定义的常量作为 Key</li>
     *   <li>避免直接使用字符串字面量</li>
     * </ul>
     *
     * @param request 请求 DTO
     * @param context 上下文
     */
    protected abstract void bind(R request, CPFlowContext context);
}
