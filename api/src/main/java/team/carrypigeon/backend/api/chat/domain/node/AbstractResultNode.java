package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * 响应组装（Result）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 从 LiteFlow 上下文中读取业务数据，构建响应 DTO 并写入
 * {@link CPFlowKeys#RESPONSE}，供 Controller 返回给客户端。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 调用 {@link #build(CPFlowContext)} 构建响应 DTO
 * 2. 将响应写入 {@link CPFlowKeys#RESPONSE}
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("ApiMessageListResult")
 * public class ApiMessageListResultNode extends AbstractResultNode<MessageListResponse> {
 *
 *     @Override
 *     protected MessageListResponse build(CPFlowContext context) {
 *         CPMessage[] messages = requireContext(context, CPNodeMessageKeys.MESSAGE_LIST);
 *         List<MessageItem> items = Arrays.stream(messages)
 *             .map(this::toItem)
 *             .toList();
 *         return new MessageListResponse(items, nextCursor, hasMore);
 *     }
 * }
 * }</pre>
 *
 * <h2>设计说明</h2>
 * <ul>
 *   <li>Result 节点是 Chain 的最后一个业务节点</li>
 *   <li>负责将内部数据模型转换为 API 契约格式</li>
 *   <li>可进行数据脱敏、格式转换等处理</li>
 * </ul>
 *
 * @param <T> 响应 DTO 类型
 * @see CPFlowKeys#RESPONSE
 */
public abstract class AbstractResultNode<T> extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractResultNode.class);

    @Override
    protected final void process(CPFlowContext context) throws Exception {
        // 1. 构建响应 DTO
        T response = build(context);

        // 2. 写入上下文
        if (response != null) {
            context.set(CPFlowKeys.RESPONSE, response);
            log.debug("[{}] 响应构建完成: type={}", getNodeId(), response.getClass().getSimpleName());
        } else {
            log.debug("[{}] 响应为空，未写入上下文", getNodeId());
        }
    }

    /**
     * 从上下文构建响应 DTO。
     * <p>
     * 子类必须实现此方法，从上下文读取业务数据并转换为响应格式。
     *
     * <h3>实现规范</h3>
     * <ul>
     *   <li>使用 {@link #requireContext} 读取必填数据</li>
     *   <li>使用 {@link #optionalContext} 读取可选数据</li>
     *   <li>遵循 API 契约定义的字段命名（snake_case）</li>
     *   <li>ID 字段应转换为 String 类型（避免 JS 精度丢失）</li>
     * </ul>
     *
     * @param context 上下文
     * @return 响应 DTO，null 表示无响应体（如 204 No Content）
     * @throws Exception 构建失败时抛出异常
     */
    protected abstract T build(CPFlowContext context) throws Exception;
}
