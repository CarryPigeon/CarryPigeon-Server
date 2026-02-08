package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * LiteFlow 业务节点（公共契约）基类。
 *
 * <h2>设计理念</h2>
 * <ul>
 *   <li>协议无关：节点只依赖 {@link CPFlowContext}，不感知 HTTP/WS 等传输细节</li>
 *   <li>统一错误处理：通过 {@link #fail(CPProblem)} 抛出标准化错误</li>
 *   <li>链路级缓存：通过 {@link #select(CPFlowContext, String, Supplier)} 避免重复查询</li>
 * </ul>
 *
 * <h2>子类实现规范</h2>
 * <ul>
 *   <li>实现 {@link #process(CPFlowContext)} 执行业务逻辑</li>
 *   <li>使用 {@link #requireContext(CPFlowContext, CPKey)} 读取必填上下文参数</li>
 *   <li>使用 {@link #fail(CPProblem)} 抛出业务错误</li>
 *   <li>使用 {@link #select(CPFlowContext, String, Supplier)} 执行可缓存的查询</li>
 * </ul>
 *
 * <h2>日志规范</h2>
 * <ul>
 *   <li>DEBUG：正常流程的关键步骤</li>
 *   <li>INFO：业务状态变更（如创建、删除）</li>
 *   <li>WARN：可恢复的异常情况</li>
 *   <li>ERROR：不可恢复的错误</li>
 * </ul>
 *
 * @see CPFlowContext
 * @see CPProblem
 */
public abstract class CPNodeComponent extends NodeComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeComponent.class);

    /**
     * LiteFlow 入口方法（框架调用）。
     * <p>
     * 从 LiteFlow Slot 获取 {@link CPFlowContext} 并委托给 {@link #process(CPFlowContext)}。
     *
     * @throws IllegalStateException 如果上下文不存在
     */
    @Override
    public final void process() throws Exception {
        CPFlowContext context = this.getContextBean(CPFlowContext.class);
        if (context == null) {
            log.error("[{}] CPFlowContext 不存在，无法执行节点", getNodeId());
            throw new IllegalStateException("CPFlowContext not found in LiteFlow slot, node=" + getNodeId());
        }
        CPSession session = null;
        Object requestData = getRequestData();
        if (requestData instanceof CPSession s) {
            session = s;
        }
        process(session, context);
    }

    /**
     * 业务处理入口（无会话）。
     * <p>
     * 适用于纯 HTTP / 定时任务等“无长连接会话”场景。
     * 默认实现为空，子类可覆盖。
     *
     * @param context LiteFlow 上下文
     * @throws Exception 处理过程中的异常
     */
    protected void process(CPFlowContext context) throws Exception {
        // 默认空实现：允许子类仅实现 {@link #process(CPSession, CPFlowContext)}
    }

    /**
     * 业务处理入口（带会话）。
     * <p>
     * 适用于长连接（Netty/WebSocket 等）场景。
     * 默认委托给 {@link #process(CPFlowContext)}。
     *
     * @param session 当前连接会话，可能为 null
     * @param context LiteFlow 上下文
     * @throws Exception 处理过程中的异常
     */
    public void process(CPSession session, CPFlowContext context) throws Exception {
        process(context);
    }

    // ==================== 上下文操作 ====================

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
     * 从上下文读取必填数据。
     * <p>
     * 如果数据不存在，记录错误日志并抛出参数校验失败异常。
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * Long userId = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
     * }</pre>
     *
     * @param context 上下文
     * @param key     数据 Key
     * @param type    数据类型（用于日志）
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
     * 从上下文读取可选数据。
     * <p>
     * 如果数据不存在，返回 null 而不抛出异常。
     *
     * @param context 上下文
     * @param key     数据 Key
     * @param <T>     返回类型
     * @return 数据值，可能为 null
     */
    protected <T> T optionalContext(CPFlowContext context, String key) {
        return context.getData(key);
    }

    /**
     * 从上下文读取可选数据（强类型 Key）。
     *
     * @param context 上下文
     * @param key     强类型 Key
     * @param <T>     返回类型
     * @return 数据值，可能为 null
     * @throws CPProblemException 类型不匹配时抛出
     */
    protected <T> T optionalContext(CPFlowContext context, CPKey<T> key) {
        if (context == null || key == null) {
            return null;
        }
        try {
            return context.get(key);
        } catch (IllegalStateException ex) {
            log.error("[{}] 上下文参数类型不匹配: key={}", getNodeId(), key.name(), ex);
            throw new CPProblemException(CPProblem.of(500, "internal_error", "context type mismatch: " + key.name()));
        }
    }

    // ==================== Bind 参数操作 ====================

    /**
     * 从 LiteFlow bind 参数读取必填配置。
     * <p>
     * bind 参数在 XML 中配置，如：{@code CPChannelSelector.bind("key","id")}
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * String mode = requireBind(CPNodeBindKeys.KEY, String.class);
     * }</pre>
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
     * 从 LiteFlow bind 参数读取可选配置。
     *
     * @param key          参数名
     * @param type         参数类型
     * @param defaultValue 默认值
     * @param <T>          返回类型
     * @return 参数值，不存在时返回默认值
     */
    protected <T> T optionalBind(String key, Class<T> type, T defaultValue) {
        T value = getBindData(key, type);
        return value != null ? value : defaultValue;
    }

    // ==================== 错误处理 ====================

    /**
     * 抛出标准化业务错误。
     * <p>
     * 此方法总是抛出 {@link CPProblemException}，用于中断流程并返回错误响应。
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * if (user == null) {
     *     fail(CPProblem.of(404, "not_found", "user not found"));
     * }
     * }</pre>
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

    /**
     * 参数校验失败的快捷方法。
     * <p>
     * 等同于 {@code fail(CPProblem.of(422, "validation_failed", "validation failed"))}
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
     * 资源未找到的快捷方法。
     *
     * @param message 错误描述
     * @throws CPProblemException 总是抛出
     */
    protected void notFound(String message) {
        fail(CPProblem.of(404, "not_found", message));
    }

    /**
     * 权限不足的快捷方法。
     *
     * @param reason  错误原因码
     * @param message 错误描述
     * @throws CPProblemException 总是抛出
     */
    protected void forbidden(String reason, String message) {
        fail(CPProblem.of(403, reason, message));
    }

    // ==================== 查询缓存 ====================

    /**
     * 执行带缓存的查询。
     * <p>
     * 同一 Chain 内相同 cacheKey 的查询只执行一次。
     *
     * @param context  上下文
     * @param cacheKey 缓存 Key
     * @param queryFn  查询函数
     * @param <T>      返回类型
     * @return 查询结果
     * @see CPFlowContext#select(String, Supplier)
     */
    protected <T> T select(CPFlowContext context, String cacheKey, Supplier<T> queryFn) {
        return context.select(cacheKey, queryFn);
    }

    /**
     * 构建单字段查询的缓存 Key。
     * <p>
     * 格式：{@code table-field:field=value}
     *
     * <h3>示例</h3>
     * <pre>{@code
     * String key = buildSelectKey("user", "id", 123);
     * // 结果: "user-id:id=123"
     * }</pre>
     *
     * @param table 表名/实体名
     * @param field 字段名
     * @param value 字段值
     * @return 缓存 Key
     */
    protected String buildSelectKey(String table, String field, Object value) {
        String safeTable = table != null ? table : "";
        String safeField = field != null ? field : "";
        return safeTable + "-" + safeField + ":" + safeField + "=" + value;
    }

    /**
     * 构建多字段查询的缓存 Key。
     * <p>
     * 格式：{@code table-f1_f2:f1=v1;f2=v2}（字段按字母序排列）
     *
     * <h3>示例</h3>
     * <pre>{@code
     * String key = buildSelectKey("channel_member", Map.of("cid", 1, "uid", 2));
     * // 结果: "channel_member-cid_uid:cid=1;uid=2"
     * }</pre>
     *
     * @param table  表名/实体名
     * @param fields 字段名-值映射
     * @return 缓存 Key
     */
    protected String buildSelectKey(String table, Map<String, ?> fields) {
        String safeTable = table != null ? table : "";
        if (fields == null || fields.isEmpty()) {
            return safeTable + "-:";
        }
        String joinedNames = fields.keySet().stream()
                .sorted()
                .collect(Collectors.joining("_"));
        String joinedPairs = fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
        return safeTable + "-" + joinedNames + ":" + joinedPairs;
    }
}
