package team.carrypigeon.backend.chat.domain.features.message.domain.api.event;

/**
 * 已读位置更新事件载荷。
 *
 * @param cid 频道 ID
 * @param uid 账号 ID
 * @param lastReadMid 最后已读消息 ID
 * @param lastReadTime 最后已读时间，epoch 毫秒
 */
public record ReadStateUpdatedPayload(String cid, String uid, String lastReadMid, long lastReadTime) {
}
