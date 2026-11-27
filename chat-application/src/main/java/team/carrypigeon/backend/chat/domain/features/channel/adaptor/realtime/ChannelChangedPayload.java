package team.carrypigeon.backend.chat.domain.features.channel.adaptor.realtime;

/**
 * 频道变化事件的线上载荷。
 * 职责：定义 `channel.changed` 事件的对外字段。
 * 边界：仅协议适配层使用，不参与领域判断。
 *
 * @param cid 频道 ID，线上为字符串
 * @param scope 失效范围的线上取值
 * @param hint 刷新提示
 */
public record ChannelChangedPayload(String cid, String scope, String hint) {
}
