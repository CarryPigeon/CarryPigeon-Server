package team.carrypigeon.backend.chat.domain.features.channel.domain.event;

/**
 * 账号可见频道集合已变化事件。
 * 职责：表达"指定账号需要重新读取自己的频道集合"这一已发生事实。
 * 边界：只承载账号标识，不描述 realtime 协议与通知偏好实现；与 {@link ChannelChangedEvent} 的区别是它以账号而非频道为视角。
 *
 * @param accountId 频道集合发生变化的账号 ID
 */
public record AccountChannelsChangedEvent(long accountId) {
}
