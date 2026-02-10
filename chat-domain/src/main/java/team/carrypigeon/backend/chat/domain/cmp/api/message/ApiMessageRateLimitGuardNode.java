package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import java.util.Map;

/**
 * 消息创建限流节点（HTTP：{@code POST /api/channels/{cid}/messages}）。
 *
 * <p>设计目标：对齐 PRD 的“服务端限流”要求，减少刷屏/滥用带来的写放大与推送压力。
 *
 * <p>实现策略：固定窗口计数器（Fixed Window），计数数据存放在 {@link CPCache} 中。
 * <ul>
 *   <li>{@code Core:Text}：维度为 {@code uid + cid}</li>
 *   <li>非 {@code Core:*}：维度为 {@code uid + cid + domain}</li>
 * </ul>
 *
 * <p>失败语义：触发限流时返回 {@code 429 rate_limited}，并在 {@code details.retry_after_ms} 给出建议重试时间。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiMessageRateLimitGuard")
public class ApiMessageRateLimitGuardNode extends CPNodeComponent {

    /** Core 文本域（服务端内置、强约束）。 */
    private static final String CORE_TEXT_DOMAIN = "Core:Text";
    /** Core 文本域限流 key 前缀。 */
    private static final String KEY_PREFIX_CORE_TEXT = "msg_rate:core_text:uid_cid:";
    /** 插件域限流 key 前缀。 */
    private static final String KEY_PREFIX_PLUGIN = "msg_rate:plugin:uid_cid_domain:";

    /** 限流计数存储（通常为 Redis/内存缓存）。 */
    private final CPCache cache;
    /** API 配置项（含限流开关与窗口参数）。 */
    private final CpApiProperties properties;

    /**
     * 执行限流检查：命中限流时中断链路并返回 {@code 429 rate_limited}。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPFlowKeys#SESSION_UID}</li>
     *   <li>{@link CPNodeChannelKeys#CHANNEL_INFO_ID}</li>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DOMAIN}</li>
     * </ul>
     */
    @Override
    protected void process(CPFlowContext context) {
        CpApiProperties.MessageRateLimit cfg = properties == null ? null : properties.getApi().getMessageRateLimit();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }

        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        String domain = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_DOMAIN);

        boolean coreText = CORE_TEXT_DOMAIN.equals(domain);
        CpApiProperties.RateLimitWindow window = coreText ? cfg.getCoreText() : cfg.getPlugin();
        if (window == null || window.getWindowSeconds() <= 0 || window.getMaxRequests() <= 0) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long nowSeconds = nowMillis / 1000L;
        int windowSeconds = window.getWindowSeconds();
        long bucketStartSeconds = (nowSeconds / windowSeconds) * windowSeconds;
        long bucketEndSeconds = bucketStartSeconds + windowSeconds;

        String key = buildKey(coreText, uid, cid, domain, bucketStartSeconds);
        int ttlSeconds = (int) Math.max(1L, (bucketEndSeconds - nowSeconds) + 1L);
        int count = increaseAndGet(key, ttlSeconds);

        if (count > window.getMaxRequests()) {
            long retryAfterMs = Math.max(1L, bucketEndSeconds * 1000L - nowMillis);
            log.warn("消息创建限流：触发：uid={}, cid={}, domain={}, count={}, limit={}, windowSeconds={}, retryAfterMs={}",
                    uid, cid, domain, count, window.getMaxRequests(), windowSeconds, retryAfterMs);
            fail(CPProblem.of(CPProblemReason.RATE_LIMITED, "too many requests", Map.of("retry_after_ms", retryAfterMs)));
        }
    }

    /**
     * 构建限流计数 key。
     *
     * <p>注意：domain 会被做简单归一化（替换 {@code :} / {@code /}），用于保证 key 可读且避免分隔符冲突。
     */
    private String buildKey(boolean coreText, long uid, long cid, String domain, long bucketStartSeconds) {
        if (coreText) {
            return KEY_PREFIX_CORE_TEXT + uid + ":" + cid + ":" + bucketStartSeconds;
        }
        String safeDomain = domain == null ? "null" : domain.replace(':', '_').replace('/', '_');
        return KEY_PREFIX_PLUGIN + uid + ":" + cid + ":" + safeDomain + ":" + bucketStartSeconds;
    }

    /**
     * 固定窗口计数自增。
     *
     * <p>说明：依赖 cache.increment 的原子实现；
     * 默认 Redis 实现会在首次自增时补齐 TTL。
     *
     * @param key 限流计数 key
     * @param ttlSeconds key TTL（秒）
     * @return 自增后的计数值（>=1）
     */
    private int increaseAndGet(String key, int ttlSeconds) {
        long next = cache.increment(key, 1L, ttlSeconds);
        if (next <= 0) {
            return 1;
        }
        if (next > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) next;
    }
}
