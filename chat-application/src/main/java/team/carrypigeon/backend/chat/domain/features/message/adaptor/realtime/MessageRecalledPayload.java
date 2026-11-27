package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 消息撤回事件载荷。
 *
 * @param cid 频道 ID
 * @param mid 消息 ID
 * @param recallTime 撤回时间，epoch 毫秒
 */
public record MessageRecalledPayload(@JsonSerialize(using = ToStringSerializer.class) long cid, @JsonSerialize(using = ToStringSerializer.class) long mid, long recallTime) {
}
