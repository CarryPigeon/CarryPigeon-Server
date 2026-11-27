package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 新消息事件载荷。
 *
 * @param cid 频道 ID
 * @param message 消息体
 */
public record MessageCreatedPayload(@JsonSerialize(using = ToStringSerializer.class) long cid, MessageBody message) {
}
