package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result;

/** 当前成员的频道已读锚点与未读数。尚未上报时锚点为空，未读数最多计至 10000。 */
public record ChannelReadSummaryResult(long channelId, long accountId, Long lastReadMessageId, long unreadCount) {
}
