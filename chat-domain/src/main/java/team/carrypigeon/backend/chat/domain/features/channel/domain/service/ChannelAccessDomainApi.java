package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccessApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
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
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道访问领域服务。
 * 职责：承接默认频道、system 频道与读状态写入等访问侧用例。
 * 边界：这里只处理单个账户与频道访问相关的编排，不承担成员治理或邀请流程。
 */
@Service
public class ChannelAccessDomainApi extends AbstractChannelDomainSupport implements ChannelAccessApi {

    public ChannelAccessDomainApi(
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
     * 获取全局默认频道。
     * 约束：调用方账号必须有效；默认频道不存在时按资源不存在处理。
     *
     * @param command 默认频道访问命令
     * @return 默认频道投影
     */
    public ChannelResult getDefaultChannel(GetDefaultChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        Channel channel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        return toResult(channel);
    }

    /**
     * 获取当前账号可访问的 system 频道。
     * 约束：system 频道要求显式成员身份，非成员按权限不足处理。
     *
     * @param command system 频道访问命令
     * @return system 频道投影
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
     * 更新账号在频道内的已读位置。
     * 副作用：写入或刷新频道已读状态，并在事务提交后发布已读状态变化事件。
     *
     * @param command 已读状态更新命令
     * @return 更新后的已读状态投影
     */
    public ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command) {
        validateUpdateChannelReadStateCommand(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            requireMember(channel.id(), command.accountId());
            MessageReferenceResult message = requireMessage(command.lastReadMessageId());
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
            channelAfterCommitPublisher.publishReadStateUpdatedAfterCommit(afterCommit, updated);
            return new ChannelReadStateResult(
                    Ids.toString(updated.channelId()),
                    Ids.toString(updated.accountId()),
                    Ids.toString(updated.lastReadMessageId()),
                    updated.lastReadTime().toEpochMilli()
            );
        });
    }
}
