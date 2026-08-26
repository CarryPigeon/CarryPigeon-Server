package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelApplicationFlowApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道申请流领域服务。
 * 职责：承接成员邀请、入群申请、审批与接受邀请等流程编排。
 * 边界：这里只处理 invite/application 生命周期，不承担频道目录查询或成员治理。
 */
@Service
public class ChannelApplicationFlowDomainApi implements ChannelApplicationFlowApi {

    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelInviteRepository channelInviteRepository;
    private final ChannelBanRepository channelBanRepository;
    private final UserProfileApi userProfileApi;
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final ChannelAfterCommitPublisher channelAfterCommitPublisher;
    private final ChannelCommandValidator channelCommandValidator = new ChannelCommandValidator();
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationFlowDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            UserProfileApi userProfileApi,
            ChannelGovernancePolicy channelGovernancePolicy,
            ApplicationEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.channelBanRepository = channelBanRepository;
        this.userProfileApi = userProfileApi;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.channelAfterCommitPublisher = new ChannelAfterCommitPublisher(eventPublisher);
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 创建频道成员邀请。
     * 副作用：保存或刷新待处理邀请，并在事务提交后通知频道申请集和被邀请账号频道集变化。
     *
     * @param command 成员邀请命令
     * @return 待处理邀请投影
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
                    null,
                    ChannelInviteStatus.PENDING,
                    now(),
                    null
            );
            if (existingInvite == null) {
                channelInviteRepository.save(invite);
            } else {
                channelInviteRepository.update(invite);
            }
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, invite.inviteeAccountId());
            return toInviteResult(invite);
        });
    }

    /**
     * 接受频道成员邀请。
     * 副作用：新增频道成员、更新邀请状态，并在事务提交后通知申请集、成员集和账号频道集变化。
     *
     * @param command 接受邀请命令
     * @return 已接受邀请投影
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
                    invite.reason(),
                    ChannelInviteStatus.ACCEPTED,
                    invite.createdAt(),
                    now()
            );
            channelInviteRepository.update(acceptedInvite);
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toInviteResult(acceptedInvite);
        });
    }

    /**
     * 创建私有频道入群申请。
     * 约束：仅私有频道支持申请；已有成员、被封禁账号或重复待处理申请会被拒绝。
     *
     * @param command 入群申请命令
     * @return 待处理申请投影
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
            ChannelInvite existingInvite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId())
                    .orElse(null);
            if (existingInvite != null && existingInvite.status() == ChannelInviteStatus.PENDING) {
                throw ProblemException.validationFailed("pending channel invite already exists");
            }
            ChannelInvite invite = new ChannelInvite(
                    channel.id(),
                    nextId(),
                    command.accountId(),
                    channelApplicationMarkerAccountId(command.accountId()),
                    normalizeApplicationReason(command.reason()),
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
                        normalizeApplicationReason(command.reason()),
                        ChannelInviteStatus.PENDING,
                        now(),
                        null
                );
                channelInviteRepository.update(persistedInvite);
            }
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toApplicationResult(persistedInvite, command.reason());
        });
    }

    /**
     * 查询频道入群申请列表。
     * 约束：调用方必须具备邀请或审核申请的频道权限。
     *
     * @param query 入群申请列表查询
     * @return 入群申请投影列表
     */
    public List<ChannelApplicationResult> listChannelApplications(ListChannelApplicationsQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanInvite(channel, operator);
        return channelInviteRepository.findByChannelId(channel.id()).stream()
                .filter(this::isChannelApplication)
                .map(invite -> toApplicationResult(invite, null))
                .toList();
    }

    /**
     * 审批频道入群申请。
     * 副作用：更新申请状态；批准时新增频道成员，并在事务提交后通知申请集、成员集和账号频道集变化。
     *
     * @param command 入群申请审批命令
     * @return 审批后的申请投影
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
            if (decidedStatus == ChannelInviteStatus.ACCEPTED
                    && !channelMemberRepository.exists(channel.id(), invite.inviteeAccountId())) {
                channelGovernancePolicy.requireBanInactive(
                        channelBanRepository.findByChannelIdAndBannedAccountId(channel.id(), invite.inviteeAccountId()).orElse(null),
                        now()
                );
                channelMemberRepository.save(newMember(channel.id(), invite.inviteeAccountId(), ChannelMemberRole.MEMBER));
            }
            ChannelInvite updated = new ChannelInvite(
                    invite.channelId(),
                    invite.applicationId(),
                    invite.inviteeAccountId(),
                    channelApplicationMarkerAccountId(invite.inviteeAccountId()),
                    invite.reason(),
                    decidedStatus,
                    invite.createdAt(),
                    now()
            );
            channelInviteRepository.update(updated);
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "applications", snapshotChannelRecipientAccountIds(channel.id()));
            if (decidedStatus == ChannelInviteStatus.ACCEPTED) {
                channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, channel, "members", snapshotChannelRecipientAccountIds(channel.id()));
            }
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, invite.inviteeAccountId());
            return toApplicationResult(updated, null);
        });
    }

    private String normalizeApplicationReason(String reason) {
        return reason == null || reason.isBlank() ? "" : reason.trim();
    }

    private void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        channelCommandValidator.validateInviteChannelMemberCommand(command);
    }

    private void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        channelCommandValidator.validateAcceptChannelInviteCommand(command);
    }

    private void requirePositive(long value, String fieldName) {
        channelCommandValidator.requirePositive(value, fieldName);
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private void requireInviteeExists(long inviteeAccountId) {
        if (userProfileApi.getPublicUserProfiles(List.of(inviteeAccountId)).isEmpty()) {
            throw ProblemException.notFound("invitee account does not exist");
        }
    }

    private boolean isChannelApplication(ChannelInvite invite) {
        return invite.inviterAccountId() == channelApplicationMarkerAccountId(invite.inviteeAccountId());
    }

    private long channelApplicationMarkerAccountId(long inviteeAccountId) {
        return inviteeAccountId;
    }

    private ChannelInviteResult toInviteResult(ChannelInvite invite) {
        return new ChannelInviteResult(
                invite.channelId(), invite.inviteeAccountId(), invite.inviterAccountId(),
                invite.status().name(), invite.createdAt(), invite.respondedAt()
        );
    }

    private ChannelApplicationResult toApplicationResult(ChannelInvite invite, String reason) {
        return new ChannelApplicationResult(
                invite.applicationId(), invite.channelId(), invite.inviteeAccountId(),
                normalizeApplicationReason(reason == null ? invite.reason() : reason),
                invite.createdAt(), invite.status().name()
        );
    }

    private ChannelMember newMember(long channelId, long accountId, ChannelMemberRole role) {
        return new ChannelMember(channelId, accountId, role, now(), null);
    }

    private List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

    private long nextId() {
        return idGenerator.nextLongId();
    }

    private java.time.Instant now() {
        return timeProvider.nowInstant();
    }
}
