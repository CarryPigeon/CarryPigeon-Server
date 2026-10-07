package team.carrypigeon.backend.chat.domain.features.channel.domain.service.application;


import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道申请流领域服务。
 * 职责：承接成员邀请、入群申请、审批与接受邀请等流程编排。
 * 边界：这里只处理 invite/application 生命周期，不承担频道目录查询或成员治理。
 */
@Service
public class ChannelApplicationDomainApi implements ChannelApplicationApi {

    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelInviteRepository channelInviteRepository;
    private final ChannelBanRepository channelBanRepository;
    private final UserProfileApi userProfileApi;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            UserProfileApi userProfileApi,
            AfterCommitEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.channelBanRepository = channelBanRepository;
        this.userProfileApi = userProfileApi;
        this.afterCommitEventPublisher = eventPublisher;
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
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.inviteeAccountId(), "inviteeAccountId");
        if (command.operatorAccountId() == command.inviteeAccountId()) {
            throw ProblemException.validationFailed("operatorAccountId must not equal inviteeAccountId");
        }
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_invite_forbidden",
                        "channel invite requires owner or admin role"
                );
            }
            requireInviteeExists(command.inviteeAccountId());
            ChannelBan existingBan = channelBanRepository
                    .findByChannelIdAndBannedAccountId(channel.id(), command.inviteeAccountId()).orElse(null);
            if (existingBan != null
                    && existingBan.revokedAt() == null
                    && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.forbidden("forbidden", "channel ban is still active");
            }
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
                    idGenerator.nextLongId(),
                    command.inviteeAccountId(),
                    command.operatorAccountId(),
                    null,
                    ChannelInviteStatus.PENDING,
                    timeProvider.nowInstant(),
                    null
            );
            if (existingInvite == null) {
                channelInviteRepository.save(invite);
            } else {
                channelInviteRepository.update(invite);
            }
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "applications", snapshotChannelRecipientAccountIds(channel.id())));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(invite.inviteeAccountId()));
            return ChannelInviteResult.from(invite);
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
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            if (channelMemberRepository.exists(channel.id(), command.accountId())) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            ChannelBan existingBan = channelBanRepository
                    .findByChannelIdAndBannedAccountId(channel.id(), command.accountId()).orElse(null);
            if (existingBan != null
                    && existingBan.revokedAt() == null
                    && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.forbidden("forbidden", "channel ban is still active");
            }
            ChannelInvite invite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId())
                    .orElseThrow(() -> ProblemException.notFound(CHANNEL_INVITE_NOT_FOUND_MESSAGE));
            if (isChannelApplication(invite)) {
                throw ProblemException.notFound(CHANNEL_INVITE_NOT_FOUND_MESSAGE);
            }
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (invite.inviteeAccountId() != command.accountId()) {
                throw ProblemException.forbidden(
                        "channel_invite_forbidden",
                        "channel invite is not granted to current account"
                );
            }
            if (invite.status() != ChannelInviteStatus.PENDING) {
                throw ProblemException.validationFailed("channel invite is not pending");
            }
            channelMemberRepository.save(newMember(channel.id(), command.accountId(), ChannelMemberRole.MEMBER));
            ChannelInvite acceptedInvite = new ChannelInvite(
                    invite.channelId(),
                    invite.applicationId(),
                    invite.inviteeAccountId(),
                    invite.inviterAccountId(),
                    invite.reason(),
                    ChannelInviteStatus.ACCEPTED,
                    invite.createdAt(),
                    timeProvider.nowInstant()
            );
            channelInviteRepository.update(acceptedInvite);
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "applications", snapshotChannelRecipientAccountIds(channel.id())));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "members", snapshotChannelRecipientAccountIds(channel.id())));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(command.accountId()));
            return ChannelInviteResult.from(acceptedInvite);
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
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (channelMemberRepository.exists(channel.id(), command.accountId())) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            ChannelBan existingBan = channelBanRepository
                    .findByChannelIdAndBannedAccountId(channel.id(), command.accountId()).orElse(null);
            if (existingBan != null
                    && existingBan.revokedAt() == null
                    && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.forbidden("forbidden", "channel ban is still active");
            }
            ChannelInvite existingInvite = channelInviteRepository.findByChannelIdAndInviteeAccountId(channel.id(), command.accountId())
                    .orElse(null);
            if (existingInvite != null && existingInvite.status() == ChannelInviteStatus.PENDING) {
                throw ProblemException.validationFailed("pending channel invite already exists");
            }
            ChannelInvite invite = new ChannelInvite(
                    channel.id(),
                    idGenerator.nextLongId(),
                    command.accountId(),
                    channelApplicationMarkerAccountId(command.accountId()),
                    normalizeApplicationReason(command.reason()),
                    ChannelInviteStatus.PENDING,
                    timeProvider.nowInstant(),
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
                        timeProvider.nowInstant(),
                        null
                );
                channelInviteRepository.update(persistedInvite);
            }
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "applications", snapshotChannelRecipientAccountIds(channel.id())));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(command.accountId()));
            return ChannelApplicationResult.from(persistedInvite, normalizeApplicationReason(command.reason()));
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
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
            );
        }
        if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(
                    "channel_invite_forbidden",
                    "channel invite requires owner or admin role"
            );
        }
        return channelInviteRepository.findByChannelId(channel.id()).stream()
                .filter(this::isChannelApplication)
                .map(invite -> ChannelApplicationResult.from(
                        invite,
                        normalizeApplicationReason(invite.reason())
                ))
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
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_invite_forbidden",
                        "channel invite requires owner or admin role"
                );
            }
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
            boolean applicantAlreadyMember = channelMemberRepository.exists(
                    channel.id(), invite.inviteeAccountId());
            if (decidedStatus == ChannelInviteStatus.ACCEPTED && !applicantAlreadyMember) {
                ChannelBan existingBan = channelBanRepository
                        .findByChannelIdAndBannedAccountId(channel.id(), invite.inviteeAccountId()).orElse(null);
                if (existingBan != null
                        && existingBan.revokedAt() == null
                        && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                    throw ProblemException.forbidden("forbidden", "channel ban is still active");
                }
            }
            ChannelInvite updated = new ChannelInvite(
                    invite.channelId(),
                    invite.applicationId(),
                    invite.inviteeAccountId(),
                    channelApplicationMarkerAccountId(invite.inviteeAccountId()),
                    invite.reason(),
                    decidedStatus,
                    invite.createdAt(),
                    timeProvider.nowInstant()
            );
            if (!channelInviteRepository.updateIfPending(updated)) {
                throw ProblemException.conflict("application_already_processed", "channel application is already processed");
            }
            if (decidedStatus == ChannelInviteStatus.ACCEPTED && !applicantAlreadyMember) {
                channelMemberRepository.save(newMember(channel.id(), invite.inviteeAccountId(), ChannelMemberRole.MEMBER));
            }
            List<Long> recipientAccountIds = snapshotChannelRecipientAccountIds(channel.id());
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new ChannelChangedEvent(channel.id(), "applications", recipientAccountIds));
            if (decidedStatus == ChannelInviteStatus.ACCEPTED) {
                afterCommitEventPublisher.publishAfterCommit(
                        afterCommit::execute, new ChannelChangedEvent(channel.id(), "members", recipientAccountIds));
            }
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(invite.inviteeAccountId()));
            return ChannelApplicationResult.from(
                    updated,
                    normalizeApplicationReason(updated.reason())
            );
        });
    }

    private String normalizeApplicationReason(String reason) {
        return reason == null || reason.isBlank() ? "" : reason.trim();
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
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

    private ChannelMember newMember(long channelId, long accountId, ChannelMemberRole role) {
        return new ChannelMember(channelId, accountId, role, timeProvider.nowInstant());
    }

    private List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

}
