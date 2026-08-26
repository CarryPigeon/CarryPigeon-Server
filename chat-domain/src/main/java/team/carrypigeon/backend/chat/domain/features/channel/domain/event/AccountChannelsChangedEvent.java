package team.carrypigeon.backend.chat.domain.features.channel.domain.event;

/**
 * 账号可见频道集合已变化事件。
 * 职责：表达指定账号需要重新读取频道集合的已发生事实。
 * 边界：不描述 realtime 协议或通知偏好实现。
 */
public record AccountChannelsChangedEvent(long accountId) {
}
