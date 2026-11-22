package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

/**
 * 频道历史消息查询。
 * @param channelId 目标频道 ID
 * @param beforeMessageId 排他上界，为 null 时从最新消息开始
 * @param limit 本页最大条数，范围为 1 至 50
 */
public record ListChannelMessagesQuery(long channelId, Long beforeMessageId, int limit) {
}
