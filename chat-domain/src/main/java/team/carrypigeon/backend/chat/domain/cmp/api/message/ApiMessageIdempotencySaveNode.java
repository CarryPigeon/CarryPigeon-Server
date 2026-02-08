package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeApiKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 消息创建幂等保存节点（写入 {@code Idempotency-Key -> mid} 映射）。
 *
 * <p>使用场景：{@code POST /api/channels/{cid}/messages} 在消息落库后执行，
 * 将本次请求的 {@code Idempotency-Key} 绑定到已创建消息的 {@code mid}。
 */
@Slf4j
@LiteflowComponent("ApiMessageIdempotencySave")
@RequiredArgsConstructor
public class ApiMessageIdempotencySaveNode extends CPNodeComponent {

    /** 幂等映射缓存存储。 */
    private final CPCache cache;

    /**
     * 写入幂等映射：{@code (uid, cid, idempotencyKey) -> mid}。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPNodeApiKeys#IDEMPOTENCY_KEY}（可选；为空则不写入）</li>
     *   <li>{@link CPFlowKeys#SESSION_UID}</li>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO}</li>
     * </ul>
     */
    @Override
    protected void process(CPFlowContext context) {
        String key = context.get(CPNodeApiKeys.IDEMPOTENCY_KEY);
        if (key == null || key.isBlank()) {
            return;
        }
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        CPMessage message = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO);
        String cacheKey = ApiMessageIdempotencyCheckNode.cacheKey(uid, message.getCid(), key);
        cache.set(cacheKey, Long.toString(message.getId()), ApiMessageIdempotencyCheckNode.ttlSeconds());
        log.debug("消息创建幂等：写入：uid={}, cid={}, mid={}", uid, message.getCid(), message.getId());
    }

    static boolean tryMarkPending(CPCache cache, long uid, long cid, String idempotencyKey) {
        if (cache == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        String cacheKey = ApiMessageIdempotencyCheckNode.cacheKey(uid, cid, idempotencyKey);
        return cache.setIfAbsent(cacheKey, ApiMessageIdempotencyCheckNode.pendingValue(), ApiMessageIdempotencyCheckNode.ttlSeconds());
    }
}
