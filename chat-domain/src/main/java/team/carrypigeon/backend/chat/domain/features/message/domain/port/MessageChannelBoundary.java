package team.carrypigeon.backend.chat.domain.features.message.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

/**
 * 消息 feature 访问频道上下文的边界端口。
 * 职责：用消息侧语义表达频道存在性、成员权限、置顶和审计协作。
 * 边界：message 领域服务不直接理解 channel 仓储和治理模型。
 */
public interface MessageChannelBoundary {

    MessageChannel requireChannel(long channelId);

    MessageChannel requireMemberChannel(long channelId, long accountId);

    MessageChannel requireSendableChannel(long channelId, long accountId, Instant now);

    void requireSystemChannel(MessageChannel channel);

    void requireRecallPermission(long channelId, long operatorAccountId, ChannelMessage message);

    void requirePinModerationPermission(long channelId, long operatorAccountId);

    List<Long> recipientAccountIds(long channelId);

    Optional<MessageChannelPin> findPin(long channelId, long messageId);

    long countPins(long channelId);

    MessageChannelPin savePin(long pinId, long channelId, long messageId, long pinnedByAccountId, String note, Instant pinnedAt);

    void deletePin(long channelId, long messageId);

    List<MessageChannelPin> findPinsBefore(long channelId, Long cursorMessageId, int limit);

    void appendMessageRecalledAudit(long auditLogId, long channelId, long actorAccountId, long messageId, long senderAccountId, Instant occurredAt);

    /**
     * 消息侧所需的频道最小快照。
     * 职责：避免 message 领域直接依赖完整 channel 聚合模型。
     */
    record MessageChannel(long id, long conversationId, String type) {
    }

    /**
     * 消息侧所需的频道置顶快照。
     * 职责：以 message 领域语义承载置顶关系和展示信息。
     */
    record MessageChannelPin(
            long pinId,
            long channelId,
            long messageId,
            long pinnedByAccountId,
            String note,
            Instant pinnedAt
    ) {
    }
}
