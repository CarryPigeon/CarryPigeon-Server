package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result;

/**
 * 频道已读状态结果。
 *
 * @param cid 频道 ID
 * @param uid 账户 ID
 * @param lastReadMid 最后已读消息 ID
 */
public record ChannelReadStateResult(String cid, String uid, String lastReadMid) {
}
