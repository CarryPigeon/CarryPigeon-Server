package team.carrypigeon.backend.chat.domain.features.channel.adaptor.realtime;

/**
 * 账号频道集合变化事件的线上载荷。
 * 职责：定义 `channels.changed` 事件的对外字段。
 * 边界：仅协议适配层使用，不参与领域判断。
 *
 * @param hint 刷新提示
 */
public record ChannelsChangedPayload(String hint) {
}
