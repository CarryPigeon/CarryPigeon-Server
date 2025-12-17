package team.carrypigeon.backend.api.chat.domain.flow;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 自定义 LiteFlow 上下文。
 * <p>
 * 在 {@link DefaultContext} 的基础上增加链路级别的查询缓存：
 * 通过 {@link #select(String, Supplier)} 在同一条 chain 内复用相同查询结果，
 * 避免多次对同一实体进行重复 IO。
 */
@Slf4j
public class CPFlowContext extends DefaultContext {

    /**
     * 与当前连接相关的只读信息快照，例如远端 IP、端口等。
     * <p>
     * 由分发器在 chain 启动前填充，各个 Node 只能读取，不应修改。
     */
    @Getter
    @Setter
    private CPFlowConnectionInfo connectionInfo;

    /**
     * 链路内查询缓存：
     * key 由调用方根据“表名 + 条件”拼接，例如：
     * user-id:id=123, channel_member-cid_uid:cid=1;uid=2
     */
    private final Map<String, Object> selectCache = new ConcurrentHashMap<>();

    /**
     * 带缓存的查询（仅缓存“成功”结果，即非 null 返回值）。
     *
     * @param cacheKey 查询缓存 key，应唯一标识本次查询的“数据含义”
     * @param queryFn  实际查询函数
     * @param <T>      返回值类型
     * @return 已缓存的结果或本次查询结果
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
            if (log.isDebugEnabled()) {
                log.debug("CPFlowContext#select hit cache for key={}", cacheKey);
            }
            return (T) cached;
        }
        T result = queryFn.get();
        if (result != null) {
            selectCache.put(cacheKey, result);
            if (log.isDebugEnabled()) {
                log.debug("CPFlowContext#select cache put for key={}", cacheKey);
            }
        }
        return result;
    }
}
