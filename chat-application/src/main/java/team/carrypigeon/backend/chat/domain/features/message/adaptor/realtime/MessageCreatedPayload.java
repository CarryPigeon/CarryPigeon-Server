package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

/**
 * 新消息事件载荷。
 *
 * @param cid 频道 ID
 * @param message 消息体
 */
public record MessageCreatedPayload(String cid, MessageBody message) {
}
