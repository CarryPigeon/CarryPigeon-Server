package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
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
 * 频道生命周期应用服务。
 * 职责：承接频道创建、资料更新与删除等生命周期用例。
 * 边界：这里只处理频道实体本身的生命周期，不承担邀请流和成员治理。
 */
@Service
public class ChannelLifecycleApplicationService extends AbstractChannelApplicationSupport {

    private static final String PRIVATE_CHANNEL_TYPE = "private";

    public ChannelLifecycleApplicationService(
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

    public ChannelResult createPrivateChannel(CreatePrivateChannelCommand command) {
        validateCreatePrivateChannelCommand(command);
        return createChannel(new CreateChannelCommand(command.accountId(), command.name(), "", ""));
    }

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
            return null;
        });
    }

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
}
