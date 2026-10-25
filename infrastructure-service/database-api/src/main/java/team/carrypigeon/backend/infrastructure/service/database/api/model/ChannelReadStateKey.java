package team.carrypigeon.backend.infrastructure.service.database.api.model;

/**
 * 频道已读状态复合主键。
 * 职责：为 `chat_channel_read_state` 的 findById / delete / deleteAll 提供稳定键对象。
 * 边界：只表达表主键，不承载已读业务语义。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 */
public record ChannelReadStateKey(long channelId, long accountId) {
}
