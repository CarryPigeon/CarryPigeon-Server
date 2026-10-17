package team.carrypigeon.backend.chat.domain.features.channel.domain.event;

import java.util.List;

/**
 * 频道资料或成员状态已变化事件。
 * 职责：向其它 feature 暴露"某频道需要刷新视图"这一已发生事实。
 * 边界：只携带频道标识、变化范围与事务内确定的接收账号快照；不携带命令语义，不用于反向命令生产者，也不保证投递可靠性。
 *
 * @param channelId 发生变化的频道 ID
 * @param scope 变化范围；空值按 profile 处理，当前取值包括 profile、members、applications、bans
 * @param recipientAccountIds 需要接收通知的账号快照；构造时做防御性拷贝，不接受外部可变列表
 */
public record ChannelChangedEvent(long channelId, String scope, List<Long> recipientAccountIds) {

    public ChannelChangedEvent {
        // 范围缺失时按资料变化处理，避免下游按空字符串分支而漏发通知
        scope = scope == null || scope.isBlank() ? "profile" : scope;
        // 接收账号快照必须不可变，防止发布后被调用方改写而影响订阅者
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}
