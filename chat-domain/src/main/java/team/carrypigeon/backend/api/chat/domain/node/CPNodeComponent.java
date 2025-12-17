package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * LiteFlow 普通节点基类。
 * <p>
 * 抽取从上下文中获取 {@link CPSession} 的通用逻辑，
 * 子类只需要实现 {@link #process(CPSession, CPFlowContext)} 执行业务。
 * <p>
 * 同时对常见错误场景（必填参数缺失、业务失败等）统一打日志，便于排查。
 */
public abstract class CPNodeComponent extends NodeComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeComponent.class);

    @Override
    public void process() throws Exception {
        CPFlowContext context = this.getContextBean(CPFlowContext.class);
        if (context == null) {
            throw new IllegalStateException("CPFlowContext not found in LiteFlow slot, node=" + getNodeId());
        }
        // 从上下文中获取当前会话 CPSession
        CPSession session = context.getData(CPNodeCommonKeys.SESSION);
        // 委托子类执行业务逻辑
        process(session, context);
    }

    /**
     * 具体节点的执行业务逻辑。
     *
     * @param session 当前连接的会话信息
     * @param context LiteFlow 上下文
     */
    public abstract void process(CPSession session, CPFlowContext context) throws Exception;

    /**
     * 从上下文中读取必填数据，如果为 null 则视为参数错误并中断流程。
     */
    protected <T> T requireContext(CPFlowContext context, String key, Class<T> type) throws CPReturnException {
        T value = context.getData(key);
        if (value == null) {
            log.error("requireContext missing key '{}' (type={}) in node {}",
                    key, type == null ? null : type.getSimpleName(), getNodeId());
            argsError(context);
        }
        return value;
    }

    /**
     * 从绑定参数中读取必填配置，如果为 null 则视为参数错误并中断流程。
     */
    protected <T> T requireBind(CPFlowContext context, String key, Class<T> type) throws CPReturnException {
        T value = getBindData(key, type);
        if (value == null) {
            log.error("requireBind missing bind key '{}' (type={}) in node {}",
                    key, type == null ? null : type.getSimpleName(), getNodeId());
            argsError(context);
        }
        return value;
    }

    /**
     * 业务失败时的统一处理：写入错误响应并中断流程。
     */
    protected void businessError(CPFlowContext context, String message) throws CPReturnException {
        log.error("businessError in node {}: {}", getNodeId(), message);
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.error(message)
        );
        throw new CPReturnException();
    }

    /**
     * 参数异常时的统一处理：写入错误响应并中断流程。
     *
     * @param context LiteFlow 上下文
     * @throws CPReturnException 用于提前终止 LiteFlow 流程
     */
    protected void argsError(CPFlowContext context) throws CPReturnException {
        log.error("argsError in node {}: invalid or missing arguments", getNodeId());
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.error("error args")
        );
        throw new CPReturnException();
    }

    /**
     * 统一入口：通过 {@link CPFlowContext#select(String, java.util.function.Supplier)} 做链路级缓存查询。
     */
    protected <T> T select(CPFlowContext context, String cacheKey, java.util.function.Supplier<T> queryFn) {
        return context.select(cacheKey, queryFn);
    }

    /**
     * 构建单字段查询的缓存 key。
     * 形如：{@code table-field:field=value}，例如 {@code user-id:id=123}
     */
    protected String buildSelectKey(String table, String field, Object value) {
        String safeTable = table == null ? "" : table;
        String safeField = field == null ? "" : field;
        return safeTable + "-" + safeField + ":" + safeField + "=" + String.valueOf(value);
    }

    /**
     * 构建多字段查询的缓存 key。
     * 形如：{@code table-f1_f2: f1=v1;f2=v2}，例如 {@code channel_member-cid_uid:cid=1;uid=2}
     */
    protected String buildSelectKey(String table, Map<String, ?> fields) {
        String safeTable = table == null ? "" : table;
        if (fields == null || fields.isEmpty()) {
            return safeTable + "-:";
        }
        String joinedNames = fields.keySet()
                .stream()
                .sorted()
                .collect(Collectors.joining("_"));
        String joinedPairs = fields.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
        return safeTable + "-" + joinedNames + ":" + joinedPairs;
    }
}
