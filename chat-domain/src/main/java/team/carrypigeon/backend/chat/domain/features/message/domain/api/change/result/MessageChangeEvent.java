package team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result;

/**
 * 一条频道消息变更，消息内容需要另行查询。
 *
 * @param eventId 事件 ID
 * @param channelId 所属频道 ID
 * @param messageId 发生变更的消息 ID
 * @param sequence 事件位置
 * @param type 事件类型
 * @param createdAt 事件产生时间，Unix 时间戳，单位为毫秒
 */
public record MessageChangeEvent(long eventId, long channelId, long messageId, long sequence, String type, long createdAt) {
}
