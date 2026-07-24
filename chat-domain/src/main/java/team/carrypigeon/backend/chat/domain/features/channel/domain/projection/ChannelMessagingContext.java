package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 消息用例所需的频道上下文。
 *
 * @param id 频道 ID
 * @param conversationId 会话 ID
 * @param type 频道类型
 */
public record ChannelMessagingContext(long id, long conversationId, String type) {
}
