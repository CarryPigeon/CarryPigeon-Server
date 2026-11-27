package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 已读位置更新事件载荷。
 *
 * @param cid 频道 ID
 * @param uid 账号 ID
 * @param lastReadMid 最后已读消息 ID
 */
public record ReadStateUpdatedPayload(@JsonSerialize(using = ToStringSerializer.class) long cid, @JsonSerialize(using = ToStringSerializer.class) long uid, @JsonSerialize(using = ToStringSerializer.class) long lastReadMid) {
}
