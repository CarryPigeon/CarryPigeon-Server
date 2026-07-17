package team.carrypigeon.backend.chat.domain.features.message.support.channel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 基于 channel feature 的消息频道边界适配器。
 * 职责：把 message 侧的频道访问端口映射到 channel 仓储与治理策略。
 * 边界：只处理跨 feature 协作，不承载消息持久化和实时发布。
 */
@Component
public class ChannelBackedMessageChannelBoundary implements MessageChannelBoundary {

    private static final String SYSTEM_MESSAGE_TYPE = "system";
    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String MESSAGE_RECALLED_AUDIT_ACTION = "MESSAGE_RECALLED";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelPinRepository channelPinRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;

    public ChannelBackedMessageChannelBoundary(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelPinRepository = channelPinRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
    }

    @Override
    public MessageChannel requireChannel(long channelId) {
        return toMessageChannel(loadChannel(channelId));
    }

    @Override
    public MessageChannel requireMemberChannel(long channelId, long accountId) {
        Channel channel = loadChannel(channelId);
        requireMembership(channel.id(), accountId);
        return toMessageChannel(channel);
    }

    @Override
    public MessageChannel requireSendableChannel(long channelId, long accountId, Instant now) {
        Channel channel = loadChannel(channelId);
        ChannelMember member = requireMembership(channel.id(), accountId);
        channelGovernancePolicy.requireCanSendMessage(channel, member, now);
        return toMessageChannel(channel);
    }

    @Override
    public void requireSystemChannel(MessageChannel channel) {
        if (!SYSTEM_MESSAGE_TYPE.equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_required", "system message requires system channel");
        }
    }

    @Override
    public void requireRecallPermission(long channelId, long operatorAccountId, ChannelMessage message) {
        Channel channel = loadChannel(channelId);
        ChannelMember operator = requireMembership(channel.id(), operatorAccountId);
        if (message.channelId() != channel.id()) {
            throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
        }
        ChannelMember senderMember = channelMemberRepository.findByChannelIdAndAccountId(channel.id(), message.senderId())
                .orElse(null);
        channelGovernancePolicy.requireCanRecallMessage(channel, operator, message.senderId(), senderMember);
    }

    @Override
    public void requirePinModerationPermission(long channelId, long operatorAccountId) {
        Channel channel = loadChannel(channelId);
        ChannelMember operator = requireMembership(channel.id(), operatorAccountId);
        channelGovernancePolicy.requireCanModeratePin(channel, operator);
    }

    @Override
    public List<Long> recipientAccountIds(long channelId) {
        return channelMemberRepository.findAccountIdsByChannelId(channelId);
    }

    @Override
    public Optional<MessageChannelPin> findPin(long channelId, long messageId) {
        return channelPinRepository.findByChannelIdAndMessageId(channelId, messageId)
                .map(this::toMessageChannelPin);
    }

    @Override
    public long countPins(long channelId) {
        return channelPinRepository.countByChannelId(channelId);
    }

    @Override
    public MessageChannelPin savePin(long pinId, long channelId, long messageId, long pinnedByAccountId, String note, Instant pinnedAt) {
        ChannelPin pin = new ChannelPin(pinId, channelId, messageId, pinnedByAccountId, note, pinnedAt);
        channelPinRepository.save(pin);
        return toMessageChannelPin(pin);
    }

    @Override
    public void deletePin(long channelId, long messageId) {
        channelPinRepository.delete(channelId, messageId);
    }

    @Override
    public void deletePinsByMessageId(long messageId) {
        channelPinRepository.deleteByMessageId(messageId);
    }

    @Override
    public List<MessageChannelPin> findPinsBefore(long channelId, Long cursorMessageId, int limit) {
        return channelPinRepository.findByChannelIdBefore(channelId, cursorMessageId, limit).stream()
                .map(this::toMessageChannelPin)
                .toList();
    }

    @Override
    public void appendMessageRecalledAudit(
            long auditLogId,
            long channelId,
            long actorAccountId,
            long messageId,
            long senderAccountId,
            Instant occurredAt
    ) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                auditLogId,
                channelId,
                actorAccountId,
                MESSAGE_RECALLED_AUDIT_ACTION,
                senderAccountId,
                "{\"messageId\":" + messageId + ",\"senderAccountId\":" + senderAccountId + "}",
                occurredAt
        ));
    }

    /**
     * 加载并要求频道存在。
     * 失败语义：频道不存在时按消息领域依赖的频道资源不存在处理。
     *
     * @param channelId 频道 ID
     * @return 频道领域对象
     */
    private Channel loadChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    /**
     * 要求账号是频道成员。
     * 失败语义：成员关系不存在时按频道成员权限不足处理。
     *
     * @param channelId 频道 ID
     * @param accountId 账号 ID
     * @return 频道成员关系
     */
    private ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private MessageChannel toMessageChannel(Channel channel) {
        return new MessageChannel(channel.id(), channel.conversationId(), channel.type());
    }

    private MessageChannelPin toMessageChannelPin(ChannelPin pin) {
        return new MessageChannelPin(
                pin.pinId(),
                pin.channelId(),
                pin.messageId(),
                pin.pinnedByAccountId(),
                pin.note(),
                pin.pinnedAt()
        );
    }
}
