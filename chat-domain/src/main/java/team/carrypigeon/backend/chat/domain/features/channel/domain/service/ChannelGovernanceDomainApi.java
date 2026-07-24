package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道治理领域服务。
 * 职责：承接成员角色治理、禁言、踢出、封禁与所有权转移等写侧用例。
 * 边界：这里只处理成员治理动作，不承担频道生命周期和申请流编排。
 */
@Service
public class ChannelGovernanceDomainApi extends AbstractChannelDomainSupport implements ChannelGovernanceApi {

    public ChannelGovernanceDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            MessageReferenceApi messageReferenceApi,
            UserProfileApi userProfileApi,
            ChannelGovernancePolicy channelGovernancePolicy,
            RealtimeEventApi realtimeEventApi,
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
                messageReferenceApi,
                userProfileApi,
                channelGovernancePolicy,
                realtimeEventApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    /**
     * 将普通成员提升为频道管理员。
     * 副作用：更新成员角色、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 成员提升命令
     * @return 提升后的成员投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(promotedMember);
        });
    }

    /**
     * 将频道管理员降级为普通成员。
     * 副作用：更新成员角色、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 管理员降级命令
     * @return 降级后的成员投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(demotedMember);
        });
    }

    /**
     * 转移频道所有权。
     * 副作用：原所有者降为管理员、目标成员升为所有者，追加审计日志并通知成员集变化。
     *
     * @param command 所有权转移命令
     * @return 所有权转移结果投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return new ChannelOwnershipTransferResult(
                    channel.id(),
                    previousOwner.accountId(),
                    previousOwner.role().name(),
                    newOwner.accountId(),
                    newOwner.role().name()
            );
        });
    }

    /**
     * 临时禁言频道成员。
     * 副作用：更新成员禁言截止时间、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 成员禁言命令
     * @return 禁言后的成员投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(mutedMember);
        });
    }

    /**
     * 解除频道成员禁言。
     * 副作用：清除成员禁言截止时间、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 解除禁言命令
     * @return 解除禁言后的成员投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            return toMemberResult(unmutedMember);
        });
    }

    /**
     * 将成员踢出频道。
     * 副作用：删除成员关系、追加审计日志，并在事务提交后通知频道成员集和目标账号频道集变化。
     *
     * @param command 踢出成员命令
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", recipientAccountIds);
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, target.accountId());
            return null;
        });
    }

    /**
     * 封禁频道成员。
     * 副作用：保存封禁记录、移除成员关系、追加审计日志，并在事务提交后通知封禁集和目标账号频道集变化。
     *
     * @param command 成员封禁命令
     * @return 封禁结果投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "bans", recipientAccountIds);
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, target.accountId());
            return toBanResult(ban);
        });
    }

    /**
     * 按绝对过期时间封禁频道成员。
     * 职责：把协议层传入的毫秒时间戳转换为领域内部持续秒数，统一使用项目时间源。
     *
     * @param command 按绝对过期时间表达的封禁命令
     * @return 封禁结果
     */
    public ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command) {
        Long durationSeconds = null;
        if (command.untilEpochMillis() != null) {
            long nowMillis = timeProvider.nowMillis();
            if (command.untilEpochMillis() <= nowMillis) {
                throw ProblemException.validationFailed("until must be greater than current time");
            }
            durationSeconds = Math.max(1L, (command.untilEpochMillis() - nowMillis) / 1000L);
        }
        return banChannelMember(new BanChannelMemberCommand(
                command.operatorAccountId(),
                command.channelId(),
                command.targetAccountId(),
                command.reason(),
                durationSeconds
        ));
    }

    /**
     * 解除频道成员封禁。
     * 副作用：标记封禁记录撤销、追加审计日志，并在事务提交后通知封禁集和目标账号频道集变化。
     *
     * @param command 解除封禁命令
     * @return 撤销后的封禁结果投影
     */
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "bans", snapshotChannelRecipientAccountIds(channel.id()));
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, existingBan.bannedAccountId());
            return toBanResult(revokedBan);
        });
    }
}
