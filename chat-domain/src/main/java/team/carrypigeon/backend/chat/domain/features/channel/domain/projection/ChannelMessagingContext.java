package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 消息用例所需的频道上下文。
 *
 * @param id 频道 ID
 * @param systemChannel 是否为系统频道
 */
public record ChannelMessagingContext(long id, boolean systemChannel) {
}
