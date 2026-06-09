package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
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
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.DiscoverChannelsQuery;
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
 * 频道应用服务。
 * 职责：编排默认频道查询、私有频道创建、邀请接受与成员查询用例。
 * 边界：当前阶段聚焦 private channel 的最小治理切片，不扩展为完整群管理平台。
 */
@Service
public class ChannelApplicationService {

    private static final String PUBLIC_CHANNEL_TYPE = "public";
    private static final String PRIVATE_CHANNEL_TYPE = "private";
    private static final String SYSTEM_CHANNEL_TYPE = "system";
    private static final String CHANNEL_NOT_FOUND_MESSAGE = "default channel does not exist";
    private static final String SYSTEM_CHANNEL_NOT_FOUND_MESSAGE = "system channel does not exist";
    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";
    private static final String CHANNEL_MEMBER_NOT_FOUND_MESSAGE = "channel member does not exist";
    private static final String CHANNEL_BAN_NOT_FOUND_MESSAGE = "channel ban does not exist";
    private static final String SYSTEM_CHANNEL_MEMBERS_HIDDEN_MESSAGE = "system channel member list is not available";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelInviteRepository channelInviteRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelReadStateRepository channelReadStateRepository;
    private final MessageRepository messageRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final ChannelRealtimePublisher channelRealtimePublisher;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationService(
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

    /**
     * 查询当前服务端默认频道。
     *
     * @param command 默认频道查询命令
     * @return 默认频道结果
     */
    public ChannelResult getDefaultChannel(GetDefaultChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        Channel channel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        return toResult(channel);
    }

    /**
     * 查询当前服务端 canonical system 频道。
     *
     * @param command system 频道查询命令
     * @return system 频道结果
     */
    public ChannelResult getSystemChannel(GetSystemChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        Channel channel = channelRepository.findSystemChannel()
                .orElseThrow(() -> ProblemException.notFound(SYSTEM_CHANNEL_NOT_FOUND_MESSAGE));
        if (!channelMemberRepository.exists(channel.id(), command.accountId())) {
            throw ProblemException.forbidden("system_channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
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
        return createChannel(new CreateChannelCommand(command.accountId(), command.name(), "", ""));
    }

    /**
     * 创建频道。
     *
     * @param command 创建命令
     * @return 已创建频道结果
     */
    public ChannelResult createChannel(CreateChannelCommand command) {
        validateCreateChannelCommand(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            long channelId = nextId();
            Channel channel = new Channel(
                    channelId,
                    channelId,
                    command.name().trim(),
                    normalizeNullableText(command.brief()),
                    normalizeNullableText(command.avatar()),
                    "",
                    PRIVATE_CHANNEL_TYPE,
                    false,
                    now(),
                    now()
            );
            channelRepository.save(channel);
            channelMemberRepository.save(newMember(channel.id(), command.accountId(), ChannelMemberRole.OWNER));
            publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toResult(channel);
        });
    }

    /**
     * 删除频道。
     *
     * @param command 删除命令
     */
    public void deleteChannel(DeleteChannelCommand command) {
        validateDeleteChannelCommand(command);
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden("channel_owner_required", "channel action requires owner role");
            }
            requireChannelDeleteSafe(channel.id());
            try {
                channelMemberRepository.findByChannelId(channel.id()).forEach(member ->
                        channelMemberRepository.delete(channel.id(), member.accountId())
                );
                channelRepository.delete(channel.id());
            } catch (ProblemException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw ProblemException.conflict(
                        "channel_delete_blocked",
                        "channel contains dependent data and cannot be deleted"
                );
            }
            publishChannelsChangedAfterCommit(afterCommit, operator.accountId());
        });
    }

    /**
     * 更新频道资料。
     *
     * @param command 更新命令
     * @return 已更新频道结果
     */
    public ChannelResult updateChannelProfile(UpdateChannelProfileCommand command) {
        validateUpdateChannelProfileCommand(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            channelGovernancePolicy.requireCanUpdateChannelProfile(channel, operator);
            Channel updated = new Channel(
                    channel.id(),
                    channel.conversationId(),
                    command.name().trim(),
                    normalizeNullableText(command.brief()),
                    channel.avatar(),
                    findOwnerUid(channel.id()),
                    channel.type(),
                    channel.defaultChannel(),
                    channel.createdAt(),
                    now()
            );
            channelRepository.update(updated);
            publishChannelChangedAfterCommit(afterCommit, updated, "profile", snapshotChannelRecipientAccountIds(channel.id()));
            return toResult(updated);
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
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            channelGovernancePolicy.requireCanInvite(channel, operator);
            requireInviteeExists(command.inviteeAccountId());
            channelGovernancePolicy.requireBanInactive(
                    channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.inviteeAccountId()).orElse(null),
                    now()
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
                    nextId(),
                    command.inviteeAccountId(),
                    command.operatorAccountId(),
                    ChannelInviteStatus.PENDING,
                    now(),
                    null
            );
            if (existingInvite == null) {
                channelInviteRepository.save(invite);
            } else {
                channelInviteRepository.update(invite);
            }
            publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelsChangedAfterCommit(afterCommit, invite.inviteeAccountId());
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
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            if (channelMemberRepository.exists(channel.id(), command.accountId())) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            channelGovernancePolicy.requireBanInactive(
                    channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.accountId()).orElse(null),
                    now()
            );
            ChannelInvite invite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId())
                    .orElseThrow(() -> ProblemException.notFound(CHANNEL_INVITE_NOT_FOUND_MESSAGE));
            if (isChannelApplication(invite)) {
                throw ProblemException.notFound(CHANNEL_INVITE_NOT_FOUND_MESSAGE);
            }
            channelGovernancePolicy.requireCanAcceptInvite(channel, invite, command.accountId());
            channelMemberRepository.save(newMember(channel.id(), command.accountId(), ChannelMemberRole.MEMBER));
            ChannelInvite acceptedInvite = new ChannelInvite(
                    invite.channelId(),
                    invite.applicationId(),
                    invite.inviteeAccountId(),
                    invite.inviterAccountId(),
                    ChannelInviteStatus.ACCEPTED,
                    invite.createdAt(),
                    now()
            );
            channelInviteRepository.update(acceptedInvite);
            publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toInviteResult(acceptedInvite);
        });
    }

    /**
     * 创建 private channel 入群申请。
     * 输入：申请人账户、目标频道和申请原因。
     * 输出：当前申请在公开协议下的结果视图。
     * 副作用：会写入或重置一条 `PENDING` 申请记录。
     */
    public ChannelApplicationResult createChannelApplication(CreateChannelApplicationCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            channelGovernancePolicy.requirePrivateChannel(channel);
            if (channelMemberRepository.exists(channel.id(), command.accountId())) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            channelGovernancePolicy.requireBanInactive(
                    channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), command.accountId()).orElse(null),
                    now()
            );
            ChannelInvite existingInvite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId()).orElse(null);
            if (existingInvite != null && existingInvite.status() == ChannelInviteStatus.PENDING) {
                throw ProblemException.validationFailed("pending channel invite already exists");
            }
            ChannelInvite invite = new ChannelInvite(
                    channel.id(),
                    nextId(),
                    command.accountId(),
                    channelApplicationMarkerAccountId(command.accountId()),
                    ChannelInviteStatus.PENDING,
                    now(),
                    null
            );
            ChannelInvite persistedInvite = invite;
            if (existingInvite == null) {
                channelInviteRepository.save(invite);
            } else {
                persistedInvite = new ChannelInvite(
                        existingInvite.channelId(),
                        existingInvite.applicationId(),
                        existingInvite.inviteeAccountId(),
                        channelApplicationMarkerAccountId(existingInvite.inviteeAccountId()),
                        ChannelInviteStatus.PENDING,
                        now(),
                        null
                );
                channelInviteRepository.update(persistedInvite);
            }
            publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toApplicationResult(persistedInvite, command.reason());
        });
    }

    /**
     * 列出指定 private channel 当前可见的申请记录。
     * 约束：只有具备邀请权限的成员才能查看。
     * 输出：结果只暴露申请视图，不返回底层 invite 持久化细节。
     */
    public List<ChannelApplicationResult> listChannelApplications(ListChannelApplicationsQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanInvite(channel, operator);
        return channelInviteRepository.findByChannelId(channel.id()).stream()
                .filter(this::isChannelApplication)
                .map(invite -> toApplicationResult(invite, ""))
                .toList();
    }

    /**
     * 审批 private channel 入群申请。
     * 输入：操作人、目标频道、申请 ID 与审批决策。
     * 副作用：可能写入成员关系并把申请状态从 `PENDING` 更新为最终态。
     * 失败：重复审批会返回冲突错误。
     */
    public ChannelApplicationResult decideChannelApplication(DecideChannelApplicationCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.applicationId(), "applicationId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            channelGovernancePolicy.requireCanInvite(channel, operator);
            ChannelInvite invite = channelInviteRepository.findByChannelIdAndApplicationId(channel.id(), command.applicationId())
                    .orElseThrow(() -> ProblemException.notFound("channel application does not exist"));
            if (!isChannelApplication(invite)) {
                throw ProblemException.notFound("channel application does not exist");
            }
            if (invite.status() != ChannelInviteStatus.PENDING) {
                throw ProblemException.conflict("application_already_processed", "channel application is already processed");
            }
            ChannelInviteStatus decidedStatus = switch (command.decision().trim().toLowerCase()) {
                case "approve" -> ChannelInviteStatus.ACCEPTED;
                case "reject" -> ChannelInviteStatus.DECLINED;
                default -> throw ProblemException.validationFailed("decision must be approve or reject");
            };
            if (decidedStatus == ChannelInviteStatus.ACCEPTED && !channelMemberRepository.exists(channel.id(), invite.inviteeAccountId())) {
                channelMemberRepository.save(newMember(channel.id(), invite.inviteeAccountId(), ChannelMemberRole.MEMBER));
            }
            ChannelInvite updated = new ChannelInvite(
                    invite.channelId(),
                    invite.applicationId(),
                    invite.inviteeAccountId(),
                    channelApplicationMarkerAccountId(invite.inviteeAccountId()),
                    decidedStatus,
                    invite.createdAt(),
                    now()
            );
            channelInviteRepository.update(updated);
            publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            if (decidedStatus == ChannelInviteStatus.ACCEPTED) {
                publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            }
            publishChannelsChangedAfterCommit(afterCommit, invite.inviteeAccountId());
            return toApplicationResult(updated, "");
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
        if (SYSTEM_CHANNEL_TYPE.equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_members_hidden", SYSTEM_CHANNEL_MEMBERS_HIDDEN_MESSAGE);
        }
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanListMembers(operator);
        return channelMemberRepository.findByChannelId(channel.id()).stream()
                .map(this::toMemberResult)
                .toList();
    }

    /**
     * 查询频道当前生效的封禁列表。
     * 约束：只有具备解封权限的成员可读取。
     * 输出：返回面向管理界面的封禁摘要，不暴露内部仓储结构。
     */
    public List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanUnban(channel, operator);
        return channelBanRepository.findByChannelId(channel.id()).stream()
                .map(ban -> new ChannelBanListItemResult(
                        ban.channelId(),
                        ban.bannedAccountId(),
                        ban.expiresAt(),
                        ban.reason(),
                        ban.createdAt()
                ))
                .toList();
    }

    /**
     * 按过滤条件查询审计日志。
     * 输入：分页游标、频道、操作者、动作与时间范围。
     * 输出：公开审计日志结果集合。
     * 约束：方法内部只做过滤参数规范化，不在这里补游标裁剪协议。
     */
    public List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorAuditId() != null && query.cursorAuditId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
        if (query.channelId() != null) {
            requirePositive(query.channelId(), "channelId");
        }
        if (query.actorAccountId() != null) {
            requirePositive(query.actorAccountId(), "actorAccountId");
        }
        if (query.fromTime() != null && query.fromTime() < 0) {
            throw ProblemException.validationFailed("from_time must be greater than or equal to 0");
        }
        if (query.toTime() != null && query.toTime() < 0) {
            throw ProblemException.validationFailed("to_time must be greater than or equal to 0");
        }
        return channelAuditLogRepository.list(
                        query.cursorAuditId(),
                        query.limit() + 1,
                        query.channelId(),
                        query.actorAccountId(),
                        normalizeAuditAction(query.action()),
                        query.fromTime() == null ? null : Instant.ofEpochMilli(query.fromTime()),
                        query.toTime() == null ? null : Instant.ofEpochMilli(query.toTime())
                ).stream()
                .map(log -> new AuditLogResult(
                        Ids.toString(log.auditId()),
                        Ids.toString(log.channelId()),
                        log.actorAccountId() == null ? null : Ids.toString(log.actorAccountId()),
                        toAuditAction(log.actionType()),
                        log.metadata(),
                        log.createdAt().toEpochMilli()
                ))
                .toList();
    }

    /**
     * 返回当前账户可见的频道摘要集合。
     *
     * @param accountId 当前账户 ID
     * @return 当前可见频道结果列表
     */
    public List<ChannelResult> listChannels(long accountId) {
        requirePositive(accountId, "accountId");
        java.util.ArrayList<ChannelResult> channels = new java.util.ArrayList<>();
        channelRepository.findDefaultChannel().ifPresent(channel -> channels.add(toResult(channel)));
        channelRepository.findSystemChannel()
                .filter(channel -> channelMemberRepository.exists(channel.id(), accountId))
                .ifPresent(channel -> channels.add(toResult(channel)));
        for (Long channelId : channelMemberRepository.findChannelIdsByAccountId(accountId)) {
            channelRepository.findById(channelId)
                    .filter(channel -> channels.stream().noneMatch(existing -> existing.channelId() == channel.id()))
                    .ifPresent(channel -> channels.add(toResult(channel)));
        }
        return channels;
    }

    /**
     * 按频道 ID 返回频道摘要。
     *
     * @param accountId 当前账户 ID
     * @param channelId 频道 ID
     * @return 频道结果
     */
    public ChannelResult getChannelById(long accountId, long channelId) {
        requirePositive(accountId, "accountId");
        requirePositive(channelId, "channelId");
        Channel channel = requireChannel(channelId);
        if (SYSTEM_CHANNEL_TYPE.equals(channel.type()) && !channelMemberRepository.exists(channel.id(), accountId)) {
            throw ProblemException.forbidden("system_channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
        return toResult(channel);
    }

    /**
     * 按公开发现规则返回可发现的频道摘要。
     * 输入：当前账户、关键字、类型和分页锚点。
     * 输出：客户端用于频道发现页的最小频道信息。
     * 约束：只允许发现 `docs/t` 支持的频道类型过滤值。
     */
    public List<DiscoverChannelResult> discoverChannels(DiscoverChannelsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorChannelId() != null && query.cursorChannelId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
        String normalizedType = normalizeChannelTypeFilter(query.type());
        return channelRepository.discoverChannels(
                        query.keyword() == null || query.keyword().isBlank() ? null : query.keyword().trim(),
                        query.cursorChannelId(),
                        normalizedType,
                        query.limit() + 1
                ).stream()
                .map(channel -> new DiscoverChannelResult(
                        Ids.toString(channel.id()),
                        channel.name(),
                        channel.brief(),
                        channel.avatar(),
                        channel.memberCount(),
                        channel.requiresApplication()
                ))
                .toList();
    }

    /**
     * 更新成员在频道中的最后已读消息位置。
     * 输入：账户、频道、最后已读消息和客户端确认时间。
     * 输出：更新后的已读状态结果。
     * 副作用：会写入 read-state，并向在线成员广播 `read_state.updated` 事件。
     */
    public ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command) {
        validateUpdateChannelReadStateCommand(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            requireMember(channel.id(), command.accountId());
            ChannelMessage message = requireMessage(command.lastReadMessageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            var current = channelReadStateRepository.findByChannelIdAndAccountId(channel.id(), command.accountId()).orElse(null);
            if (current != null && current.lastReadMessageId() >= command.lastReadMessageId()) {
                return new ChannelReadStateResult(
                        Ids.toString(current.channelId()),
                        Ids.toString(current.accountId()),
                        Ids.toString(current.lastReadMessageId()),
                        current.lastReadTime().toEpochMilli()
                );
            }
            Instant now = Instant.ofEpochMilli(command.lastReadTime());
            var updated = new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState(
                    channel.id(),
                    command.accountId(),
                    command.lastReadMessageId(),
                    now,
                    current == null ? now : current.createdAt(),
                    now
            );
            channelReadStateRepository.upsert(updated);
            publishReadStateUpdatedAfterCommit(afterCommit, updated);
            return new ChannelReadStateResult(
                    Ids.toString(updated.channelId()),
                    Ids.toString(updated.accountId()),
                    Ids.toString(updated.lastReadMessageId()),
                    updated.lastReadTime().toEpochMilli()
            );
        });
    }

    /**
     * 汇总账户在各频道上的未读统计。
     * 输出：每个频道的未读数量与最后已读时间。
     * 边界：这里只做结果投影，不重新计算消息可见性规则。
     */
    public List<ChannelUnreadResult> listUnreads(long accountId) {
        requirePositive(accountId, "accountId");
        return channelReadStateRepository.listUnreadsByAccountId(accountId).stream()
                .map(item -> new ChannelUnreadResult(
                        Ids.toString(item.channelId()),
                        item.unreadCount(),
                        item.lastReadTime() == null ? 0L : item.lastReadTime().toEpochMilli()
                ))
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

    /**
     * 降级 private channel ADMIN 为 MEMBER。
     *
     * @param command 降级命令
     * @return 更新后的成员结果
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
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
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

    /**
     * 解除 private channel 成员禁言。
     *
     * @param command 解除禁言命令
     * @return 更新后的成员结果
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
            publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
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

    /**
     * 解除 private channel 封禁。
     *
     * @param command 解封命令
     * @return 解封后的封禁结果
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
            publishChannelChangedAfterCommit(afterCommit, channel, "bans", snapshotChannelRecipientAccountIds(channel.id()));
            publishChannelsChangedAfterCommit(afterCommit, existingBan.bannedAccountId());
            return toBanResult(revokedBan);
        });
    }

    private void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> channelRealtimePublisher.publishReadStateUpdated(readState));
    }

    private void publishChannelChangedAfterCommit(
            AfterCommitExecutor afterCommit,
            Channel channel,
            String scope,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelChanged(channel, scope, recipientAccountIds));
    }

    private void publishChannelsChangedAfterCommit(AfterCommitExecutor afterCommit, long accountId) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelsChanged(accountId));
    }

    private List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

    private ChannelResult toResult(Channel channel) {
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

    private ChannelApplicationResult toApplicationResult(ChannelInvite invite, String reason) {
        return new ChannelApplicationResult(
                invite.applicationId(),
                invite.channelId(),
                invite.inviteeAccountId(),
                reason == null ? "" : reason,
                invite.createdAt(),
                invite.status().name()
        );
    }

    private boolean isChannelApplication(ChannelInvite invite) {
        return invite.inviterAccountId() == channelApplicationMarkerAccountId(invite.inviteeAccountId());
    }

    private long channelApplicationMarkerAccountId(long inviteeAccountId) {
        return inviteeAccountId;
    }

    private void requireChannelDeleteSafe(long channelId) {
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
        requirePositive(command.accountId(), "accountId");
        if (command.name() == null || command.name().isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (command.name().trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
    }

    private void validateCreateChannelCommand(CreateChannelCommand command) {
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

    private void validateUpdateChannelProfileCommand(UpdateChannelProfileCommand command) {
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

    private void validateDeleteChannelCommand(DeleteChannelCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    private void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.inviteeAccountId(), "inviteeAccountId");
        if (command.operatorAccountId() == command.inviteeAccountId()) {
            throw ProblemException.validationFailed("operatorAccountId must not equal inviteeAccountId");
        }
    }

    private void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
    }

    private void validateListChannelMembersQuery(ListChannelMembersQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
    }

    private void validateUpdateChannelReadStateCommand(UpdateChannelReadStateCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.lastReadMessageId(), "lastReadMessageId");
        if (command.lastReadTime() <= 0) {
            throw ProblemException.validationFailed("lastReadTime must be greater than 0");
        }
    }

    private void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId, String targetFieldName) {
        requirePositive(operatorAccountId, "operatorAccountId");
        requirePositive(channelId, "channelId");
        requirePositive(targetAccountId, targetFieldName);
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal " + targetFieldName);
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private String findOwnerUid(long channelId) {
        return channelMemberRepository.findByChannelId(channelId).stream()
                .filter(member -> member.role() == ChannelMemberRole.OWNER)
                .map(member -> Ids.toString(member.accountId()))
                .findFirst()
                .orElse("");
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
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
                nextId(),
                channelId,
                actorAccountId,
                actionType,
                targetAccountId,
                metadata == null ? "{}" : metadata,
                now()
        ));
    }

    private ChannelMember newMember(long channelId, long accountId, ChannelMemberRole role) {
        return new ChannelMember(channelId, accountId, role, now(), null);
    }

    private long nextId() {
        return idGenerator.nextLongId();
    }

    private Instant now() {
        return timeProvider.nowInstant();
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    private String normalizeAuditAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return switch (action.trim()) {
            case "channel.create" -> "CHANNEL_CREATED";
            case "channel.delete" -> "CHANNEL_DELETED";
            case "channel.update" -> "CHANNEL_UPDATED";
            case "channel.member.kick" -> "MEMBER_KICKED";
            case "channel.admin.grant" -> "MEMBER_PROMOTED_TO_ADMIN";
            case "channel.admin.revoke" -> "ADMIN_DEMOTED_TO_MEMBER";
            case "channel.ban.create" -> "MEMBER_BANNED";
            case "channel.ban.delete" -> "MEMBER_UNBANNED";
            case "message.delete" -> "MESSAGE_DELETED";
            case "message.edit" -> "MESSAGE_EDITED";
            case "message.pin" -> "MESSAGE_PINNED";
            case "message.unpin" -> "MESSAGE_UNPINNED";
            default -> throw ProblemException.validationFailed("action is invalid");
        };
    }

    private String normalizeChannelTypeFilter(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalizedType = type.trim().toLowerCase();
        if (!List.of(PUBLIC_CHANNEL_TYPE, PRIVATE_CHANNEL_TYPE, SYSTEM_CHANNEL_TYPE).contains(normalizedType)) {
            throw ProblemException.validationFailed("type is invalid");
        }
        return normalizedType;
    }

    private String toAuditAction(String actionType) {
        return switch (actionType) {
            case "CHANNEL_CREATED" -> "channel.create";
            case "CHANNEL_DELETED" -> "channel.delete";
            case "CHANNEL_UPDATED" -> "channel.update";
            case "MEMBER_KICKED" -> "channel.member.kick";
            case "MEMBER_PROMOTED_TO_ADMIN" -> "channel.admin.grant";
            case "ADMIN_DEMOTED_TO_MEMBER" -> "channel.admin.revoke";
            case "MEMBER_BANNED" -> "channel.ban.create";
            case "MEMBER_UNBANNED" -> "channel.ban.delete";
            case "MESSAGE_DELETED" -> "message.delete";
            case "MESSAGE_EDITED" -> "message.edit";
            case "MESSAGE_PINNED" -> "message.pin";
            case "MESSAGE_UNPINNED" -> "message.unpin";
            default -> actionType.toLowerCase();
        };
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return reason.trim();
    }

    private String normalizeNullableText(String value) {
        return value == null ? "" : value.trim();
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
