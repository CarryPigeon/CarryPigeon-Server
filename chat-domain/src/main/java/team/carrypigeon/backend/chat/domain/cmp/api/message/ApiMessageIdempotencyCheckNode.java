package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeApiKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 消息创建幂等校验节点（支持 HTTP {@code Idempotency-Key}）。
 *
 * <p>使用场景：{@code POST /api/channels/{cid}/messages}。当客户端在网络抖动/重试时携带相同
 * {@code Idempotency-Key}，服务端可返回“同一次创建”的结果，避免重复写库与重复推送。
 *
 * <p>输出：
 * <ul>
 *   <li>{@link CPFlowKeys#CHECK_RESULT}：其 {@code msg} 为 {@code "hit"} 或 {@code "miss"}</li>
 *   <li>当命中时：同时写入 {@link CPNodeMessageKeys#MESSAGE_INFO} 供结果节点复用</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("ApiMessageIdempotencyCheck")
@RequiredArgsConstructor
public class ApiMessageIdempotencyCheckNode extends CPNodeComponent {

    /** 幂等映射的 TTL（秒）：避免长期占用缓存空间。 */
    private static final int TTL_SECONDS = 600;

    /** 幂等映射缓存存储。 */
    private final CPCache cache;
    /** 消息查询 DAO（用于命中时返回已创建消息）。 */
    private final ChannelMessageDao channelMessageDao;

    /**
     * 判断当前请求的 {@code Idempotency-Key} 是否已完成（命中则复用历史消息）。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPNodeApiKeys#IDEMPOTENCY_KEY}（可选；为空视为 miss）</li>
     *   <li>{@link CPFlowKeys#SESSION_UID}</li>
     *   <li>{@link CPNodeChannelKeys#CHANNEL_INFO_ID}</li>
     * </ul>
     *
     * <p>输出：
     * <ul>
     *   <li>{@link CPFlowKeys#CHECK_RESULT}</li>
     *   <li>命中时：{@link CPNodeMessageKeys#MESSAGE_INFO}</li>
     * </ul>
     */
    @Override
    protected void process(CPFlowContext context) {
        String key = context.get(CPNodeApiKeys.IDEMPOTENCY_KEY);
        if (key == null || key.isBlank()) {
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, "miss"));
            return;
        }

        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        String cacheKey = cacheKey(uid, cid, key);
        String midStr = cache.get(cacheKey);
        if (midStr == null || midStr.isBlank()) {
            boolean marked = ApiMessageIdempotencySaveNode.tryMarkPending(cache, uid, cid, key);
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, marked ? "miss" : "pending"));
            return;
        }

        if (pendingValue().equals(midStr)) {
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, "pending"));
            return;
        }

        long mid;
        try {
            mid = Long.parseLong(midStr);
        } catch (Exception e) {
            cache.delete(cacheKey);
            boolean marked = ApiMessageIdempotencySaveNode.tryMarkPending(cache, uid, cid, key);
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, marked ? "miss" : "pending"));
            return;
        }

        CPMessage existing = channelMessageDao.getById(mid);
        if (existing == null) {
            cache.delete(cacheKey);
            boolean marked = ApiMessageIdempotencySaveNode.tryMarkPending(cache, uid, cid, key);
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, marked ? "miss" : "pending"));
            return;
        }
        if (existing.getCid() != cid || existing.getUid() != uid) {
            cache.delete(cacheKey);
            boolean marked = ApiMessageIdempotencySaveNode.tryMarkPending(cache, uid, cid, key);
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, marked ? "miss" : "pending"));
            return;
        }

        context.set(CPNodeMessageKeys.MESSAGE_INFO, existing);
        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, "hit"));
        log.debug("消息创建幂等：命中：uid={}, cid={}, mid={}", uid, cid, mid);
    }

    /**
     * 构建幂等映射缓存 key。
     *
     * <p>key 维度：{@code uid + cid + idempotencyKey}，避免跨用户/跨频道碰撞导致“串消息”风险。
     */
    static String cacheKey(long uid, long cid, String key) {
        return "cp:api:idem:msg:" + uid + ":" + cid + ":" + key;
    }

    /**
     * 幂等映射的 TTL（秒）。
     */
    static int ttlSeconds() {
        return TTL_SECONDS;
    }

    static String pendingValue() {
        return "PENDING";
    }

}
