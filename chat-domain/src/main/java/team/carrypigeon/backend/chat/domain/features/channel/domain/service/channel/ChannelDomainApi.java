package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;


import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道生命周期领域服务。
 * 职责：承接频道创建、资料更新与删除等生命周期用例。
 * 边界：这里只处理频道实体本身的生命周期，不承担邀请流和成员治理。
 */
@Service
public class ChannelDomainApi implements ChannelApi {

    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelApplicationRepository channelApplicationRepository;
    private final ChannelBanRepository channelBanRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        ChannelApplicationRepository channelApplicationRepository,
        ChannelBanRepository channelBanRepository,
        AfterCommitEventPublisher eventPublisher,
        IdGenerator idGenerator,
        TimeProvider timeProvider,
        TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelApplicationRepository = channelApplicationRepository;
        this.channelBanRepository = channelBanRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<ChannelResult> listChannels(long accountId) {
        // 可见性唯一标准：账号在当前频道存在成员映射
        List<ChannelMember> memberships = channelMemberRepository.listByAccountId(accountId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Channel> channels = channelRepository.listByIds(
            memberships.stream().map(ChannelMember::channelId).toList());
        return channels.stream()
            .map(ChannelResult::from)
            .toList();
    }

    @Override
    public ChannelResult getChannelById(long accountId, long channelId) {
        // 查询对应的频道
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
        // 判断是否有查询权限：统一以成员映射为准
        if (channelMemberRepository.findByRelation(channel.id(), accountId).isEmpty()) {
            throw ProblemException.forbidden("channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
        // 返回对应的result
        return ChannelResult.from(channel);
    }

    /**
     * 创建频道并把创建者设为所有者。
     *
     * @param command 频道创建命令
     * @return 创建后的频道投影
     */
    @Override
    public ChannelResult createChannel(CreateChannelCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 创建频道原始信息
            long channelId = idGenerator.nextLongId();
            Channel channel = new Channel(
                channelId,
                command.name().trim(),
                command.brief() == null ? "" : command.brief().trim(),
                command.avatar() == null ? "" : command.avatar().trim(),
                false,
                timeProvider.nowInstant(),
                timeProvider.nowInstant()
            );
            // 持久化频道信息
            channelRepository.save(channel);
            // 持久化频道用户信息
            channelMemberRepository.save(new ChannelMember(idGenerator.nextLongId(), channel.id(), command.accountId(), ChannelMemberRole.OWNER, timeProvider.nowInstant()));
            // 发布频道创建事件
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(command.accountId()));
            // 返回
            return ChannelResult.from(channel);
        });
    }

    /**
     * 删除频道。
     * 约束：仅频道所有者可删除，且频道不存在申请、禁言、消息或审计等依赖数据。
     *
     * @param command 频道删除命令
     */
    @Override
    public void deleteChannel(DeleteChannelCommand command) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 查询对应的channel
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            // 查询对应的channel与member的对应关系
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            // 权限判断
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden("channel_owner_required", "channel action requires owner role");
            }
            // TODO 依赖数据没有删除
            if (
                !channelApplicationRepository.list(command.channelId()).isEmpty()
                    || !channelBanRepository.list(command.channelId()).isEmpty()
            ) {
                throw ProblemException.conflict(
                    "channel_delete_blocked",
                    "channel contains dependent data and cannot be deleted"
                );
            }
            //执行对应的删除操作
            channelMemberRepository.deleteByChannelId(channel.id());
            channelRepository.delete(channel.id());
            // TODO 通知操作成功，操作应该向所有人通知而不仅仅是对频道删除者
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(operator.accountId()));
        });
    }

    /**
     * 更新频道资料。
     * 副作用：保存频道名称和简介变化，并在事务提交后通知频道资料变化。
     *
     * @param command 频道资料更新命令
     * @return 更新后的频道投影
     */
    @Override
    public ChannelResult updateChannelProfile(UpdateChannelProfileCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询基本的信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            // 权限校验：统一按成员角色判断
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_profile_forbidden",
                    "channel action requires owner or admin role"
                );
            }
            // 创建更新后的频道信息
            Channel updated = new Channel(
                channel.id(),
                command.name().trim(),
                command.brief() == null ? "" : command.brief().trim(),
                channel.avatar(),
                false,
                channel.createdAt(),
                timeProvider.nowInstant()
            );
            // 执行更新操作
            channelRepository.update(updated);
            // 通知相关人员
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(
                    updated.id(),
                    "profile",
                    channelMemberRepository.listByChannelId(command.channelId()).stream().map(ChannelMember::accountId).toList()
                )
            );
            return ChannelResult.from(updated);
        });
    }

}
