package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * channel 命令侧应用服务共享支撑。
 * 职责：收敛频道能力服务共用的仓储访问、校验、结果投影、审计和实时广播辅助逻辑。
 * 边界：仅供 channel feature 内部应用服务复用，不作为对外稳定入口。
 */
abstract class AbstractChannelApplicationSupport {

    protected static final String CHANNEL_NOT_FOUND_MESSAGE = "default channel does not exist";
    protected static final String SYSTEM_CHANNEL_NOT_FOUND_MESSAGE = "system channel does not exist";
    protected static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    protected static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    protected static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    protected static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";
    protected static final String CHANNEL_MEMBER_NOT_FOUND_MESSAGE = "channel member does not exist";
    protected static final String CHANNEL_BAN_NOT_FOUND_MESSAGE = "channel ban does not exist";

    protected final ChannelRepository channelRepository;
    protected final ChannelMemberRepository channelMemberRepository;
    protected final ChannelInviteRepository channelInviteRepository;
    protected final ChannelBanRepository channelBanRepository;
    protected final ChannelAuditLogRepository channelAuditLogRepository;
    protected final ChannelReadStateRepository channelReadStateRepository;
    protected final MessageRepository messageRepository;
    protected final UserProfileRepository userProfileRepository;
    protected final ChannelGovernancePolicy channelGovernancePolicy;
    protected final ChannelRealtimePublisher channelRealtimePublisher;
    protected final IdGenerator idGenerator;
    protected final TimeProvider timeProvider;
    protected final TransactionRunner transactionRunner;

    protected AbstractChannelApplicationSupport(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            MessageRepository messageRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            ChannelRealtimePublisher channelRealtimePublisher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelReadStateRepository = channelReadStateRepository;
        this.messageRepository = messageRepository;
        this.userProfileRepository = userProfileRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.channelRealtimePublisher = channelRealtimePublisher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    protected void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> channelRealtimePublisher.publishReadStateUpdated(readState));
    }

    protected void publishChannelChangedAfterCommit(
            AfterCommitExecutor afterCommit,
            Channel channel,
            String scope,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelChanged(channel, scope, recipientAccountIds));
    }

    protected void publishChannelsChangedAfterCommit(AfterCommitExecutor afterCommit, long accountId) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelsChanged(accountId));
    }

    protected List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

    protected ChannelResult toResult(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                findOwnerUid(channel.id()),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }

    protected ChannelInviteResult toInviteResult(ChannelInvite invite) {
        return new ChannelInviteResult(
                invite.channelId(),
                invite.inviteeAccountId(),
                invite.inviterAccountId(),
                invite.status().name(),
                invite.createdAt(),
                invite.respondedAt()
        );
    }

    protected ChannelApplicationResult toApplicationResult(ChannelInvite invite, String reason) {
        return new ChannelApplicationResult(
                invite.applicationId(),
                invite.channelId(),
                invite.inviteeAccountId(),
                reason == null ? "" : reason,
                invite.createdAt(),
                invite.status().name()
        );
    }

    protected boolean isChannelApplication(ChannelInvite invite) {
        return invite.inviterAccountId() == channelApplicationMarkerAccountId(invite.inviteeAccountId());
    }

    protected long channelApplicationMarkerAccountId(long inviteeAccountId) {
        return inviteeAccountId;
    }

    protected void requireChannelDeleteSafe(long channelId) {
        if (!channelInviteRepository.findByChannelId(channelId).isEmpty()
                || !channelBanRepository.findByChannelId(channelId).isEmpty()
                || !messageRepository.findByChannelIdBefore(channelId, null, 1).isEmpty()
                || !channelAuditLogRepository.list(null, 1, channelId, null, null, null, null).isEmpty()) {
            throw ProblemException.conflict(
                    "channel_delete_blocked",
                    "channel contains dependent data and cannot be deleted"
            );
        }
    }

    protected ChannelBanResult toBanResult(ChannelBan ban) {
        return new ChannelBanResult(
                ban.channelId(),
                ban.bannedAccountId(),
                ban.operatorAccountId(),
                ban.reason(),
                ban.expiresAt(),
                ban.createdAt(),
                ban.revokedAt()
        );
    }

    protected ChannelMemberResult toMemberResult(ChannelMember member) {
        UserProfile userProfile = userProfileRepository.findByAccountId(member.accountId()).orElse(null);
        return new ChannelMemberResult(
                member.accountId(),
                userProfile == null ? "" : userProfile.nickname(),
                userProfile == null ? "" : userProfile.avatarUrl(),
                member.role().name(),
                member.joinedAt(),
                member.mutedUntil()
        );
    }

    protected void validateCreatePrivateChannelCommand(CreatePrivateChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        if (command.name() == null || command.name().isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (command.name().trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
    }

    protected void validateCreateChannelCommand(CreateChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        if (command.name() == null || command.name().isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (command.name().trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
        if (command.brief() != null && command.brief().trim().length() > 256) {
            throw ProblemException.validationFailed("brief length must be less than or equal to 256");
        }
    }

    protected void validateUpdateChannelProfileCommand(UpdateChannelProfileCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        if (command.name() == null || command.name().isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (command.name().trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
        if (command.brief() != null && command.brief().trim().length() > 256) {
            throw ProblemException.validationFailed("brief length must be less than or equal to 256");
        }
    }

    protected void validateDeleteChannelCommand(DeleteChannelCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    protected void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.inviteeAccountId(), "inviteeAccountId");
        if (command.operatorAccountId() == command.inviteeAccountId()) {
            throw ProblemException.validationFailed("operatorAccountId must not equal inviteeAccountId");
        }
    }

    protected void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
    }

    protected void validateUpdateChannelReadStateCommand(UpdateChannelReadStateCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.lastReadMessageId(), "lastReadMessageId");
        if (command.lastReadTime() <= 0) {
            throw ProblemException.validationFailed("lastReadTime must be greater than 0");
        }
    }

    protected void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId, String targetFieldName) {
        requirePositive(operatorAccountId, "operatorAccountId");
        requirePositive(channelId, "channelId");
        requirePositive(targetAccountId, targetFieldName);
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal " + targetFieldName);
        }
    }

    protected Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    protected ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    protected String findOwnerUid(long channelId) {
        return channelMemberRepository.findByChannelId(channelId).stream()
                .filter(member -> member.role() == ChannelMemberRole.OWNER)
                .map(member -> Ids.toString(member.accountId()))
                .findFirst()
                .orElse("");
    }

    protected ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    protected ChannelMember requireTargetMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
    }

    protected void requireInviteeExists(long inviteeAccountId) {
        if (userProfileRepository.findByAccountId(inviteeAccountId).isEmpty()) {
            throw ProblemException.notFound("invitee account does not exist");
        }
    }

    protected void appendAuditLog(long channelId, long actorAccountId, String actionType, Long targetAccountId, String metadata) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                nextId(),
                channelId,
                actorAccountId,
                actionType,
                targetAccountId,
                metadata == null ? "{}" : metadata,
                now()
        ));
    }

    protected ChannelMember newMember(long channelId, long accountId, ChannelMemberRole role) {
        return new ChannelMember(channelId, accountId, role, now(), null);
    }

    protected long nextId() {
        return idGenerator.nextLongId();
    }

    protected Instant now() {
        return timeProvider.nowInstant();
    }

    protected void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    protected String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return reason.trim();
    }

    protected String normalizeNullableText(String value) {
        return value == null ? "" : value.trim();
    }

    protected String buildBanAuditMetadata(ChannelBan ban) {
        StringBuilder metadata = new StringBuilder("{");
        metadata.append("\"expiresAt\":");
        if (ban.expiresAt() == null) {
            metadata.append("null");
        } else {
            metadata.append("\"").append(ban.expiresAt()).append("\"");
        }
        metadata.append('}');
        return metadata.toString();
    }
}
