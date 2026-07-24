package team.carrypigeon.backend.chat.domain.features.message.domain.projection;

/**
 * 消息引用投影。
 *
 * @param messageId 消息 ID
 * @param channelId 消息所属频道 ID
 */
public record MessageReferenceResult(long messageId, long channelId) {
}
