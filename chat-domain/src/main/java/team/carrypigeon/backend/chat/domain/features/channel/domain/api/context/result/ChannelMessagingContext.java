package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;

/**
 * 消息用例所需的频道上下文。
 *
 * @param id 频道 ID
 * @param systemChannel 是否为系统频道
 */
public record ChannelMessagingContext(long id, boolean systemChannel) {

    public static ChannelMessagingContext from(Channel channel) {
        return new ChannelMessagingContext(channel.id(), channel.systemChannel());
    }
}
