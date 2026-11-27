package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 新增提及事件载荷。
 *
 * @param mentionId 提及 ID
 * @param cid 频道 ID
 * @param mid 消息 ID
 * @param fromUid 发起提及的账号 ID
 * @param uid 被提及的账号 ID
 * @param createdAt 提及创建时间，epoch 毫秒
 */
public record MentionCreatedPayload(
        @JsonSerialize(using = ToStringSerializer.class) long mentionId,
        @JsonSerialize(using = ToStringSerializer.class) long cid,
        @JsonSerialize(using = ToStringSerializer.class) long mid,
        @JsonSerialize(using = ToStringSerializer.class) long fromUid,
        @JsonSerialize(using = ToStringSerializer.class) long uid,
        long createdAt
) {
}
