package team.carrypigeon.backend.chat.domain.features.message.domain.api.event;

/**
 * 消息撤回事件载荷。
 *
 * @param cid 频道 ID
 * @param mid 消息 ID
 * @param recallTime 撤回时间，epoch 毫秒
 */
public record MessageRecalledPayload(String cid, String mid, long recallTime) {
}
