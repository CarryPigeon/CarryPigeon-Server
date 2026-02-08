package team.carrypigeon.backend.api.chat.domain.flow;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * LiteFlow 业务上下文。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>承载请求生命周期内的所有共享数据</li>
 *   <li>提供链路级查询缓存，避免重复 IO</li>
 *   <li>存储连接相关的只读信息</li>
 * </ul>
 *
 * <h2>数据存取规范</h2>
 * <ul>
 *   <li>Key 必须使用 {@link CPFlowKeys} 或 {@code CPNode*Keys} 中定义的常量</li>
 *   <li>跨节点共享的数据通过 {@link #set(CPKey, Object)} / {@link #get(CPKey)} 存取</li>
 *   <li>查询结果使用 {@link #select(String, Supplier)} 进行缓存</li>
 * </ul>
 *
 * <h2>生命周期</h2>
 * <pre>
 * 1. Controller 创建 Context，写入 REQUEST / AUTH_UID
 * 2. LiteFlow Chain 执行，各节点读写数据
 * 3. Result 节点写入 RESPONSE
 * 4. Controller 读取 RESPONSE 并返回
 * </pre>
 *
 * @see CPFlowKeys
 */
public class CPFlowContext extends DefaultContext {

    private static final Logger log = LoggerFactory.getLogger(CPFlowContext.class);

    /**
     * 连接相关的只读信息（如远端 IP、端口等）。
     * <p>
     * 由 Controller 在 Chain 启动前填充，节点只读不写。
     */
    @Getter
    @Setter
    private CPFlowConnectionInfo connectionInfo;

    /**
     * 链路级查询缓存。
     * <p>
     * Key 格式建议：{@code table-field:field=value}
     * <br>
     * 例如：{@code user-id:id=123}, {@code channel_member-cid_uid:cid=1;uid=2}
     */
    private final Map<String, Object> selectCache = new ConcurrentHashMap<>();

    // ==================== 强类型 Key 访问 ====================

    public <T> void set(CPKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        setData(key.name(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(CPKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object raw = getData(key.name());
        if (raw == null) {
            return null;
        }
        if (!key.type().isInstance(raw)) {
            throw new IllegalStateException("context type mismatch: key=" + key.name()
                    + ", expected=" + key.type().getSimpleName()
                    + ", actual=" + raw.getClass().getSimpleName());
        }
        return (T) raw;
    }

    public void remove(CPKey<?> key) {
        Objects.requireNonNull(key, "key");
        dataMap.remove(key.name());
    }

    public void remove(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        dataMap.remove(key);
    }

    /**
     * 带缓存的查询。
     * <p>
     * 在同一条 Chain 内，相同 cacheKey 的查询只会执行一次，
     * 后续调用直接返回缓存结果。
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * CPUser user = context.select(
     *     buildSelectKey("user", "id", userId),
     *     () -> userDao.getById(userId)
     * );
     * }</pre>
     *
     * <h3>缓存规则</h3>
     * <ul>
     *   <li>只缓存非 null 结果</li>
     *   <li>null 结果不缓存，每次都会重新查询</li>
     *   <li>缓存仅在当前 Chain 执行期间有效</li>
     * </ul>
     *
     * @param cacheKey 缓存 Key，应唯一标识查询条件
     * @param queryFn  实际查询函数
     * @param <T>      返回值类型
     * @return 缓存的结果或本次查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> T select(String cacheKey, Supplier<T> queryFn) {
        if (queryFn == null) {
            return null;
        }
        if (cacheKey == null || cacheKey.isEmpty()) {
            return queryFn.get();
        }

        Object cached = selectCache.get(cacheKey);
        if (cached != null) {
            log.debug("[Context] 缓存命中: key={}", cacheKey);
            return (T) cached;
        }

        T result = queryFn.get();
        if (result != null) {
            selectCache.put(cacheKey, result);
            log.debug("[Context] 缓存写入: key={}", cacheKey);
        }
        return result;
    }
}
