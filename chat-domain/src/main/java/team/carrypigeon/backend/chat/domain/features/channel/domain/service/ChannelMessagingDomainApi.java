package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息协作 API 实现。
 * 职责：在 channel feature 内完成消息用例所需的成员、治理、置顶和审计操作。
 * 边界：不读取 message 聚合或仓储。
 */
@Service
public class ChannelMessagingDomainApi implements ChannelMessagingApi {

    private static final String MESSAGE_RECALLED_AUDIT_ACTION = "MESSAGE_RECALLED";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelPinRepository channelPinRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;

    public ChannelMessagingDomainApi(
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
    public ChannelMessagingContext requireChannel(long channelId) {
        return toContext(loadChannel(channelId));
    }

    @Override
    public ChannelMessagingContext requireMemberChannel(long channelId, long accountId) {
        Channel channel = loadChannel(channelId);
        requireMembership(channel.id(), accountId);
        return toContext(channel);
    }

    @Override
    public ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now) {
        Channel channel = loadChannel(channelId);
        ChannelMember member = requireMembership(channel.id(), accountId);
        channelGovernancePolicy.requireCanSendMessage(channel, member, now);
        return toContext(channel);
    }

    @Override
    public void requireRecallPermission(RequireMessageRecallPermissionCommand command) {
        Channel channel = loadChannel(command.channelId());
        ChannelMember operator = requireMembership(channel.id(), command.operatorAccountId());
        if (command.messageChannelId() != channel.id()) {
            throw ProblemException.notFound("message does not exist");
        }
        ChannelMember senderMember = channelMemberRepository
                .findByChannelIdAndAccountId(channel.id(), command.senderAccountId())
                .orElse(null);
        channelGovernancePolicy.requireCanRecallMessage(
                channel,
                operator,
                command.senderAccountId(),
                senderMember
        );
    }

    @Override
    public void requirePinModerationPermission(long channelId, long operatorAccountId) {
        Channel channel = loadChannel(channelId);
        channelGovernancePolicy.requireCanModeratePin(channel, requireMembership(channel.id(), operatorAccountId));
    }

    @Override
    public boolean isMember(long channelId, long accountId) {
        return channelMemberRepository.exists(channelId, accountId);
    }

    @Override
    public List<Long> recipientAccountIds(long channelId) {
        return channelMemberRepository.findAccountIdsByChannelId(channelId);
    }

    @Override
    public Optional<ChannelPinReference> findPin(long channelId, long messageId) {
        return channelPinRepository.findByChannelIdAndMessageId(channelId, messageId).map(this::toPinReference);
    }

    @Override
    public long countPins(long channelId) {
        return channelPinRepository.countByChannelId(channelId);
    }

    @Override
    public ChannelPinReference savePin(
            long pinId,
            long channelId,
            long messageId,
            long pinnedByAccountId,
            String note,
            Instant pinnedAt
    ) {
        ChannelPin pin = new ChannelPin(pinId, channelId, messageId, pinnedByAccountId, note, pinnedAt);
        channelPinRepository.save(pin);
        return toPinReference(pin);
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
    public List<ChannelPinReference> findPinsBefore(long channelId, Long cursorMessageId, int limit) {
        return channelPinRepository.findByChannelIdBefore(channelId, cursorMessageId, limit).stream()
                .map(this::toPinReference)
                .toList();
    }

    @Override
    public void appendMessageRecallAudit(AppendMessageRecallAuditCommand command) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                command.auditLogId(),
                command.channelId(),
                command.actorAccountId(),
                MESSAGE_RECALLED_AUDIT_ACTION,
                command.senderAccountId(),
                "{\"messageId\":" + command.messageId()
                        + ",\"senderAccountId\":" + command.senderAccountId() + "}",
                command.occurredAt()
        ));
    }

    private Channel loadChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
    }

    private ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
    }

    private ChannelMessagingContext toContext(Channel channel) {
        return new ChannelMessagingContext(channel.id(), channel.conversationId(), channel.type());
    }

    private ChannelPinReference toPinReference(ChannelPin pin) {
        return new ChannelPinReference(
                pin.pinId(),
                pin.channelId(),
                pin.messageId(),
                pin.pinnedByAccountId(),
                pin.note(),
                pin.pinnedAt()
        );
    }
}
