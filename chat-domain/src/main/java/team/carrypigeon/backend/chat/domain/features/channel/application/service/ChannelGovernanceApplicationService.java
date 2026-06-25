package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道治理应用服务。
 * 职责：承接成员角色治理、禁言、踢出、封禁与所有权转移等写侧用例。
 * 边界：这里只处理成员治理动作，不承担频道生命周期和申请流编排。
 */
@Service
public class ChannelGovernanceApplicationService extends AbstractChannelApplicationSupport {

    public ChannelGovernanceApplicationService(
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
        super(
                channelRepository,
                channelMemberRepository,
                channelInviteRepository,
                channelBanRepository,
                channelAuditLogRepository,
                channelReadStateRepository,
                messageRepository,
                userProfileRepository,
                channelGovernancePolicy,
                channelRealtimePublisher,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    public ChannelMemberResult promoteChannelMember(PromoteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanPromoteToAdmin(channel, operator, target);
            ChannelMember promotedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.ADMIN,
                    target.joinedAt(),
                    target.mutedUntil()
            );
            channelMemberRepository.update(promotedMember);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_PROMOTED_TO_ADMIN", target.accountId(), null);
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(promotedMember);
        });
    }

    public ChannelMemberResult demoteChannelAdmin(DemoteChannelAdminCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanDemoteAdmin(channel, operator, target);
            ChannelMember demotedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.MEMBER,
                    target.joinedAt(),
                    target.mutedUntil()
            );
            channelMemberRepository.update(demotedMember);
            appendAuditLog(channel.id(), operator.accountId(), "ADMIN_DEMOTED_TO_MEMBER", target.accountId(), null);
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(demotedMember);
        });
    }

    public ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanTransferOwnership(channel, operator, target);
            ChannelMember previousOwner = new ChannelMember(
                    operator.channelId(),
                    operator.accountId(),
                    ChannelMemberRole.ADMIN,
                    operator.joinedAt(),
                    operator.mutedUntil()
            );
            ChannelMember newOwner = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.OWNER,
                    target.joinedAt(),
                    target.mutedUntil()
            );
            channelMemberRepository.update(previousOwner);
            channelMemberRepository.update(newOwner);
            appendAuditLog(
                    channel.id(),
                    operator.accountId(),
                    "CHANNEL_OWNERSHIP_TRANSFERRED",
                    target.accountId(),
                    "{\"previousOwnerAccountId\":" + operator.accountId() + "}"
            );
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return new ChannelOwnershipTransferResult(
                    channel.id(),
                    previousOwner.accountId(),
                    previousOwner.role().name(),
                    newOwner.accountId(),
                    newOwner.role().name()
            );
        });
    }

    public ChannelMemberResult muteChannelMember(MuteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        if (command.durationSeconds() <= 0) {
            throw ProblemException.validationFailed("durationSeconds must be greater than 0");
        }
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_mute_forbidden");
            Instant mutedUntil = now().plusSeconds(command.durationSeconds());
            ChannelMember mutedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    target.role(),
                    target.joinedAt(),
                    mutedUntil
            );
            channelMemberRepository.update(mutedMember);
            appendAuditLog(
                    channel.id(),
                    operator.accountId(),
                    "MEMBER_MUTED",
                    target.accountId(),
                    "{\"durationSeconds\":" + command.durationSeconds() + "}"
            );
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(mutedMember);
        });
    }

    public ChannelMemberResult unmuteChannelMember(UnmuteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_mute_forbidden");
            ChannelMember unmutedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    target.role(),
                    target.joinedAt(),
                    null
            );
            channelMemberRepository.update(unmutedMember);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_UNMUTED", target.accountId(), null);
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(unmutedMember);
        });
    }

    public void kickChannelMember(KickChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_kick_forbidden");
            List<Long> recipientAccountIds = snapshotChannelRecipientAccountIds(channel.id());
            channelMemberRepository.delete(channel.id(), target.accountId());
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_KICKED", target.accountId(), null);
            publishChannelChangedAfterCommit(afterCommit, channel, "members", recipientAccountIds);
            publishChannelsChangedAfterCommit(afterCommit, target.accountId());
            return null;
        });
    }

    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        if (command.durationSeconds() != null && command.durationSeconds() <= 0) {
            throw ProblemException.validationFailed("durationSeconds must be greater than 0");
        }
        if (command.reason() != null && command.reason().trim().length() > 256) {
            throw ProblemException.validationFailed("reason length must be less than or equal to 256");
        }
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_ban_forbidden");
            ChannelBan existingBan = channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), target.accountId()).orElse(null);
            if (channelGovernancePolicy.isBanActive(existingBan, timeProvider.nowInstant())) {
                throw ProblemException.validationFailed("channel ban is already active");
            }
            Instant now = now();
            ChannelBan ban = new ChannelBan(
                    channel.id(),
                    target.accountId(),
                    operator.accountId(),
                    normalizeReason(command.reason()),
                    command.durationSeconds() == null ? null : now.plusSeconds(command.durationSeconds()),
                    now,
                    null
            );
            if (existingBan == null) {
                channelBanRepository.save(ban);
            } else {
                channelBanRepository.update(ban);
            }
            List<Long> recipientAccountIds = snapshotChannelRecipientAccountIds(channel.id());
            channelMemberRepository.delete(channel.id(), target.accountId());
            appendAuditLog(
                    channel.id(),
                    operator.accountId(),
                    "MEMBER_BANNED",
                    target.accountId(),
                    buildBanAuditMetadata(ban)
            );
            publishChannelChangedAfterCommit(afterCommit, channel, "bans", recipientAccountIds);
            publishChannelsChangedAfterCommit(afterCommit, target.accountId());
            return toBanResult(ban);
        });
    }

    public ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            channelGovernancePolicy.requireCanUnban(channel, operator);
            ChannelBan existingBan = channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.targetAccountId())
                    .orElseThrow(() -> ProblemException.notFound(CHANNEL_BAN_NOT_FOUND_MESSAGE));
            if (!channelGovernancePolicy.isBanActive(existingBan, timeProvider.nowInstant())) {
                throw ProblemException.validationFailed("channel ban is not active");
            }
            ChannelBan revokedBan = new ChannelBan(
                    existingBan.channelId(),
                    existingBan.bannedAccountId(),
                    operator.accountId(),
                    existingBan.reason(),
                    existingBan.expiresAt(),
                    existingBan.createdAt(),
                    now()
            );
            channelBanRepository.update(revokedBan);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_UNBANNED", existingBan.bannedAccountId(), null);
            publishChannelChangedAfterCommit(afterCommit, channel, "bans", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelsChangedAfterCommit(afterCommit, existingBan.bannedAccountId());
            return toBanResult(revokedBan);
        });
    }
}
