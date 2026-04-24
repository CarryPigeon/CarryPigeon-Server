package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道应用服务。
 * 职责：编排默认频道查询、私有频道创建、邀请接受与成员查询用例。
 * 边界：当前阶段聚焦 private channel 的最小治理切片，不扩展为完整群管理平台。
 */
@Service
public class ChannelApplicationService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "default channel does not exist";
    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";
    private static final String CHANNEL_MEMBER_NOT_FOUND_MESSAGE = "channel member does not exist";
    private static final String CHANNEL_BAN_NOT_FOUND_MESSAGE = "channel ban does not exist";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelInviteRepository channelInviteRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.userProfileRepository = userProfileRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 查询当前服务端默认频道。
     *
     * @param command 默认频道查询命令
     * @return 默认频道结果
     */
    public ChannelResult getDefaultChannel(GetDefaultChannelCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        Channel channel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        return toResult(channel);
    }

    /**
     * 创建 private channel，并将创建者写入 OWNER 活跃成员投影。
     *
     * @param command 创建命令
     * @return 已创建频道结果
     */
    public ChannelResult createPrivateChannel(CreatePrivateChannelCommand command) {
        validateCreatePrivateChannelCommand(command);
        return transactionRunner.runInTransaction(() -> {
            long channelId = idGenerator.nextLongId();
            Channel channel = new Channel(
                    channelId,
                    channelId,
                    command.name().trim(),
                    "private",
                    false,
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );
            channelRepository.save(channel);
            channelMemberRepository.save(new ChannelMember(
                    channel.id(),
                    command.accountId(),
                    ChannelMemberRole.OWNER,
                    timeProvider.nowInstant(),
                    null
            ));
            return toResult(channel);
        });
    }

    /**
     * 向 private channel 邀请新成员。
     *
     * @param command 邀请命令
     * @return 邀请结果
     */
    public ChannelInviteResult inviteChannelMember(InviteChannelMemberCommand command) {
        validateInviteChannelMemberCommand(command);
        return transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            channelGovernancePolicy.requireCanInvite(channel, operator);
            requireInviteeExists(command.inviteeAccountId());
            channelGovernancePolicy.requireBanInactive(
                    channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.inviteeAccountId()).orElse(null),
                    timeProvider.nowInstant()
            );
            if (channelMemberRepository.exists(channel.id(), command.inviteeAccountId())) {
                throw ProblemException.validationFailed("invitee is already a channel member");
            }
            ChannelInvite existingInvite = channelInviteRepository.findByChannelIdAndInviteeAccountId(
                    channel.id(),
                    command.inviteeAccountId()
            ).orElse(null);
            if (existingInvite != null && existingInvite.status() == ChannelInviteStatus.PENDING) {
                throw ProblemException.validationFailed("pending channel invite already exists");
            }
            ChannelInvite invite = new ChannelInvite(
                    channel.id(),
                    command.inviteeAccountId(),
                    command.operatorAccountId(),
                    ChannelInviteStatus.PENDING,
                    timeProvider.nowInstant(),
                    null
            );
            if (existingInvite == null) {
                channelInviteRepository.save(invite);
            } else {
                channelInviteRepository.update(invite);
            }
            return toInviteResult(invite);
        });
    }

    /**
     * 接受 private channel 邀请并写入 MEMBER 活跃成员投影。
     *
     * @param command 接受邀请命令
     * @return 更新后的邀请结果
     */
    public ChannelInviteResult acceptChannelInvite(AcceptChannelInviteCommand command) {
        validateAcceptChannelInviteCommand(command);
        return transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            if (channelMemberRepository.exists(channel.id(), command.accountId())) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            channelGovernancePolicy.requireBanInactive(
                    channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.accountId()).orElse(null),
                    timeProvider.nowInstant()
            );
            ChannelInvite invite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId())
                    .orElseThrow(() -> ProblemException.notFound(CHANNEL_INVITE_NOT_FOUND_MESSAGE));
            channelGovernancePolicy.requireCanAcceptInvite(channel, invite, command.accountId());
            channelMemberRepository.save(new ChannelMember(
                    channel.id(),
                    command.accountId(),
                    ChannelMemberRole.MEMBER,
                    timeProvider.nowInstant(),
                    null
            ));
            ChannelInvite acceptedInvite = new ChannelInvite(
                    invite.channelId(),
                    invite.inviteeAccountId(),
                    invite.inviterAccountId(),
                    ChannelInviteStatus.ACCEPTED,
                    invite.createdAt(),
                    timeProvider.nowInstant()
            );
            channelInviteRepository.update(acceptedInvite);
            return toInviteResult(acceptedInvite);
        });
    }

    /**
     * 查询频道成员列表。
     *
     * @param query 查询参数
     * @return 频道成员结果列表
     */
    public List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query) {
        validateListChannelMembersQuery(query);
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanListMembers(operator);
        return channelMemberRepository.findByChannelId(channel.id()).stream()
                .map(this::toMemberResult)
                .toList();
    }

    /**
     * 提升 private channel 成员为 ADMIN。
     *
     * @param command 提升命令
     * @return 更新后的成员结果
     */
    public ChannelMemberResult promoteChannelMember(PromoteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(() -> {
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
            return toMemberResult(promotedMember);
        });
    }

    /**
     * 降级 private channel ADMIN 为 MEMBER。
     *
     * @param command 降级命令
     * @return 更新后的成员结果
     */
    public ChannelMemberResult demoteChannelAdmin(DemoteChannelAdminCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(() -> {
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
            return toMemberResult(demotedMember);
        });
    }

    /**
     * 转移 private channel 所有权，并将原 OWNER 保持为 ADMIN。
     *
     * @param command 转移命令
     * @return 所有权转移结果
     */
    public ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(() -> {
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
     * 禁言 private channel 成员。
     *
     * @param command 禁言命令
     * @return 更新后的成员结果
     */
    public ChannelMemberResult muteChannelMember(MuteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        if (command.durationSeconds() <= 0) {
            throw ProblemException.validationFailed("durationSeconds must be greater than 0");
        }
        return transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_mute_forbidden");
            Instant mutedUntil = timeProvider.nowInstant().plusSeconds(command.durationSeconds());
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
            return toMemberResult(mutedMember);
        });
    }

    /**
     * 解除 private channel 成员禁言。
     *
     * @param command 解除禁言命令
     * @return 更新后的成员结果
     */
    public ChannelMemberResult unmuteChannelMember(UnmuteChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(() -> {
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
            return toMemberResult(unmutedMember);
        });
    }

    /**
     * 踢出 private channel 成员。
     *
     * @param command 移除命令
     */
    public void kickChannelMember(KickChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_kick_forbidden");
            channelMemberRepository.delete(channel.id(), target.accountId());
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_KICKED", target.accountId(), null);
        });
    }

    /**
     * 封禁 private channel 成员，并移除其活跃成员投影。
     *
     * @param command 封禁命令
     * @return 封禁结果
     */
    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        if (command.durationSeconds() != null && command.durationSeconds() <= 0) {
            throw ProblemException.validationFailed("durationSeconds must be greater than 0");
        }
        if (command.reason() != null && command.reason().trim().length() > 256) {
            throw ProblemException.validationFailed("reason length must be less than or equal to 256");
        }
        return transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            channelGovernancePolicy.requireCanModerateMember(channel, operator, target, "channel_ban_forbidden");
            ChannelBan existingBan = channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), target.accountId()).orElse(null);
            if (channelGovernancePolicy.isBanActive(existingBan, timeProvider.nowInstant())) {
                throw ProblemException.validationFailed("channel ban is already active");
            }
            Instant now = timeProvider.nowInstant();
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
            channelMemberRepository.delete(channel.id(), target.accountId());
            appendAuditLog(
                    channel.id(),
                    operator.accountId(),
                    "MEMBER_BANNED",
                    target.accountId(),
                    buildBanAuditMetadata(ban)
            );
            return toBanResult(ban);
        });
    }

    /**
     * 解除 private channel 封禁。
     *
     * @param command 解封命令
     * @return 解封后的封禁结果
     */
    public ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(() -> {
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
                    timeProvider.nowInstant()
            );
            channelBanRepository.update(revokedBan);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_UNBANNED", existingBan.bannedAccountId(), null);
            return toBanResult(revokedBan);
        });
    }

    private ChannelResult toResult(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }

    private ChannelInviteResult toInviteResult(ChannelInvite invite) {
        return new ChannelInviteResult(
                invite.channelId(),
                invite.inviteeAccountId(),
                invite.inviterAccountId(),
                invite.status().name(),
                invite.createdAt(),
                invite.respondedAt()
        );
    }

    private ChannelBanResult toBanResult(ChannelBan ban) {
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

    private ChannelMemberResult toMemberResult(ChannelMember member) {
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

    private void validateCreatePrivateChannelCommand(CreatePrivateChannelCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (command.name().trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
    }

    private void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        if (command.operatorAccountId() <= 0) {
            throw ProblemException.validationFailed("operatorAccountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (command.inviteeAccountId() <= 0) {
            throw ProblemException.validationFailed("inviteeAccountId must be greater than 0");
        }
        if (command.operatorAccountId() == command.inviteeAccountId()) {
            throw ProblemException.validationFailed("operatorAccountId must not equal inviteeAccountId");
        }
    }

    private void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
    }

    private void validateListChannelMembersQuery(ListChannelMembersQuery query) {
        if (query.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (query.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
    }

    private void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId, String targetFieldName) {
        if (operatorAccountId <= 0) {
            throw ProblemException.validationFailed("operatorAccountId must be greater than 0");
        }
        if (channelId <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (targetAccountId <= 0) {
            throw ProblemException.validationFailed(targetFieldName + " must be greater than 0");
        }
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal " + targetFieldName);
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("channel_member_required", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private ChannelMember requireTargetMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
    }

    private void requireInviteeExists(long inviteeAccountId) {
        if (userProfileRepository.findByAccountId(inviteeAccountId).isEmpty()) {
            throw ProblemException.notFound("invitee account does not exist");
        }
    }

    private void appendAuditLog(long channelId, long actorAccountId, String actionType, Long targetAccountId, String metadata) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                idGenerator.nextLongId(),
                channelId,
                actorAccountId,
                actionType,
                targetAccountId,
                metadata == null ? "{}" : metadata,
                timeProvider.nowInstant()
        ));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return reason.trim();
    }

    private String buildBanAuditMetadata(ChannelBan ban) {
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
