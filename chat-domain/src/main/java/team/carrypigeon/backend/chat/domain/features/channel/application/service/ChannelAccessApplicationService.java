package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
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
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道访问应用服务。
 * 职责：承接默认频道、system 频道与读状态写入等访问侧用例。
 * 边界：这里只处理单个账户与频道访问相关的编排，不承担成员治理或邀请流程。
 */
@Service
public class ChannelAccessApplicationService extends AbstractChannelApplicationSupport {

    public ChannelAccessApplicationService(
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

    public ChannelResult getDefaultChannel(GetDefaultChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        Channel channel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        return toResult(channel);
    }

    public ChannelResult getSystemChannel(GetSystemChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        Channel channel = channelRepository.findSystemChannel()
                .orElseThrow(() -> ProblemException.notFound(SYSTEM_CHANNEL_NOT_FOUND_MESSAGE));
        if (!channelMemberRepository.exists(channel.id(), command.accountId())) {
            throw ProblemException.forbidden("system_channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
        return toResult(channel);
    }

    public ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command) {
        validateUpdateChannelReadStateCommand(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            requireMember(channel.id(), command.accountId());
            ChannelMessage message = requireMessage(command.lastReadMessageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            ChannelReadState current = channelReadStateRepository.findByChannelIdAndAccountId(channel.id(), command.accountId())
                    .orElse(null);
            if (current != null && current.lastReadMessageId() >= command.lastReadMessageId()) {
                return new ChannelReadStateResult(
                        Ids.toString(current.channelId()),
                        Ids.toString(current.accountId()),
                        Ids.toString(current.lastReadMessageId()),
                        current.lastReadTime().toEpochMilli()
                );
            }
            Instant now = Instant.ofEpochMilli(command.lastReadTime());
            ChannelReadState updated = new ChannelReadState(
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
}
