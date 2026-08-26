package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;

/**
 * 频道事实事件的 realtime 映射监听器。
 * 职责：把 channel 拥有的事实事件转换为既有 realtime 发布命令。
 * 边界：不参与频道事务和业务决策，按 Spring 默认同步事件语义执行。
 */
@Component
public class ChannelRealtimeEventListener {

    private final RealtimeEventApi realtimeEventApi;

    public ChannelRealtimeEventListener(RealtimeEventApi realtimeEventApi) {
        this.realtimeEventApi = realtimeEventApi;
    }

    @EventListener
    public void onChannelChanged(ChannelChangedEvent event) {
        realtimeEventApi.publish(new PublishRealtimeEventCommand(
                event.channelId(),
                "channel.changed",
                Map.of("cid", Long.toString(event.channelId()), "scope", event.scope(), "hint", "refresh"),
                event.recipientAccountIds(),
                true
        ));
    }

    @EventListener
    public void onAccountChannelsChanged(AccountChannelsChangedEvent event) {
        realtimeEventApi.publish(new PublishRealtimeEventCommand(
                null, "channels.changed", Map.of("hint", "refresh"), List.of(event.accountId()), false
        ));
    }
}
