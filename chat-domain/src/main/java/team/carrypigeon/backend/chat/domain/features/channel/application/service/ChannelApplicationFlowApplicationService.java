package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
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
 * 频道申请流应用服务。
 * 职责：承接成员邀请、入群申请、审批与接受邀请等流程编排。
 * 边界：这里只处理 invite/application 生命周期，不承担频道目录查询或成员治理。
 */
@Service
public class ChannelApplicationFlowApplicationService extends AbstractChannelApplicationSupport {

    public ChannelApplicationFlowApplicationService(
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
}
