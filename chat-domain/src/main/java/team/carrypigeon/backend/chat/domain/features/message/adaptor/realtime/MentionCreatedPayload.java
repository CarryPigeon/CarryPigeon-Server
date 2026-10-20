package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

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
        String mentionId,
        String cid,
        String mid,
        String fromUid,
        String uid,
        long createdAt
) {
}
