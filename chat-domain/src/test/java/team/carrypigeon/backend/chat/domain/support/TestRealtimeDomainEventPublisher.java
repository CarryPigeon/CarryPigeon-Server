package team.carrypigeon.backend.chat.domain.support;

import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessagePinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageUnpinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.ReadStateUpdatedEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.ChannelRealtimeEventListener;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.MessageRealtimeEventListener;

/**
 * 领域事件到 realtime 命令的同步测试路由器。
 * 职责：在不启动 Spring 容器的业务测试中复用正式 server 监听器映射。
 * 边界：只支持当前正式 channel/message 事实事件，未知事件立即失败以暴露装配遗漏。
 */
public final class TestRealtimeDomainEventPublisher implements ApplicationEventPublisher {

    private final ChannelRealtimeEventListener channelListener;
    private final MessageRealtimeEventListener messageListener;

    public TestRealtimeDomainEventPublisher(RealtimeEventApi realtimeEventApi) {
        this.channelListener = new ChannelRealtimeEventListener(realtimeEventApi);
        this.messageListener = new MessageRealtimeEventListener(realtimeEventApi);
    }

    @Override
    public void publishEvent(Object event) {
        switch (event) {
            case ChannelChangedEvent value -> channelListener.onChannelChanged(value);
            case AccountChannelsChangedEvent value -> channelListener.onAccountChannelsChanged(value);
            case MessageCreatedEvent value -> messageListener.onMessageCreated(value);
            case MessageRecalledEvent value -> messageListener.onMessageRecalled(value);
            case MessagePinnedEvent value -> messageListener.onMessagePinned(value);
            case MessageUnpinnedEvent value -> messageListener.onMessageUnpinned(value);
            case MentionCreatedEvent value -> messageListener.onMentionCreated(value);
            case ReadStateUpdatedEvent value -> messageListener.onReadStateUpdated(value);
            default -> throw new IllegalArgumentException("unsupported test domain event: " + event.getClass().getName());
        }
    }
}
