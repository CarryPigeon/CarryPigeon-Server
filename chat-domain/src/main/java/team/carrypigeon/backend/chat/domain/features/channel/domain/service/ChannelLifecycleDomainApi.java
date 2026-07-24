package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
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
 * 频道生命周期领域服务。
 * 职责：承接频道创建、资料更新与删除等生命周期用例。
 * 边界：这里只处理频道实体本身的生命周期，不承担邀请流和成员治理。
 */
@Service
public class ChannelLifecycleDomainApi extends AbstractChannelDomainSupport implements ChannelLifecycleApi {

    private static final String PRIVATE_CHANNEL_TYPE = "private";

    public ChannelLifecycleDomainApi(
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
     * 创建当前账号拥有的私有频道。
     * 语义：复用通用频道创建流程，私有频道当前使用空简介和空头像初始化。
     *
     * @param command 私有频道创建命令
     * @return 创建后的频道投影
     */
    public ChannelResult createPrivateChannel(CreatePrivateChannelCommand command) {
        validateCreatePrivateChannelCommand(command);
        return createChannel(new CreateChannelCommand(command.accountId(), command.name(), "", ""));
    }

    /**
     * 创建频道并把创建者设为所有者。
     * 副作用：保存频道、保存 owner 成员关系，并在事务提交后通知创建者频道集变化。
     *
     * @param command 频道创建命令
     * @return 创建后的频道投影
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
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, command.accountId());
            return toResult(channel);
        });
    }

    /**
     * 删除频道。
     * 约束：仅频道所有者可删除，且频道不存在邀请、封禁、消息或审计等依赖数据。
     *
     * @param command 频道删除命令
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
            channelAfterCommitPublisher.publishChannelsChangedAfterCommit(afterCommit, operator.accountId());
            return null;
        });
    }

    /**
     * 更新频道资料。
     * 副作用：保存频道名称和简介变化，并在事务提交后通知频道资料变化。
     *
     * @param command 频道资料更新命令
     * @return 更新后的频道投影
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
            channelAfterCommitPublisher.publishChannelChangedAfterCommit(afterCommit, updated, "profile", snapshotChannelRecipientAccountIds(channel.id()));
            return toResult(updated);
        });
    }
}
