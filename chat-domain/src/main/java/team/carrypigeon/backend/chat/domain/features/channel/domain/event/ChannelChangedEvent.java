package team.carrypigeon.backend.chat.domain.features.channel.domain.event;

import java.util.List;

/**
 * 频道资料或成员状态已变化事件。
 * 职责：向其它 feature 暴露需要刷新频道视图的已发生事实。
 * 边界：只携带频道标识、变化范围和事务内确定的接收账号快照。
 */
public record ChannelChangedEvent(long channelId, String scope, List<Long> recipientAccountIds) {

    public ChannelChangedEvent {
        scope = scope == null || scope.isBlank() ? "profile" : scope;
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}
