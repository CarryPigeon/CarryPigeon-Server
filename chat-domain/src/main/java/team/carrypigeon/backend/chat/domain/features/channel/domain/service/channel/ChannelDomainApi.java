package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelChangeScope;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道生命周期领域服务。
 * 职责：承接频道创建、资料更新与删除等生命周期用例。
 * 边界：这里只处理频道实体本身的生命周期，不承担邀请流和成员治理。
 */
@Service
public class ChannelDomainApi implements ChannelApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelApplicationRepository channelApplicationRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelEventNotifier channelEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        ChannelApplicationRepository channelApplicationRepository,
        ChannelBanRepository channelBanRepository,
        ChannelEventNotifier channelEventNotifier,
        IdGenerator idGenerator,
        TimeProvider timeProvider,
        TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelApplicationRepository = channelApplicationRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelEventNotifier = channelEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public PageResult<ChannelResult> listChannels(long accountId, PageQuery page) {
        // 可见性唯一标准：账号在当前频道存在成员映射，分页也落在成员关系表上
        PageResult<ChannelMember> memberships = channelMemberRepository.pageByAccountId(accountId, page);
        if (memberships.items().isEmpty()) {
            return new PageResult<>(List.of(), memberships.page(), memberships.pageSize(), memberships.total());
        }
        List<Channel> channels = channelRepository.listByIds(
                memberships.items().stream().map(ChannelMember::channelId).toList());
        Map<Long, Channel> channelById = channels.stream()
                .collect(Collectors.toMap(Channel::id, channel -> channel));
        // 数据库 IN 查询不保证顺序，这里按成员关系分页结果重排，保证翻页边界稳定
        List<ChannelResult> items = memberships.items().stream()
                .map(member -> channelById.get(member.channelId()))
                .filter(Objects::nonNull)
                .map(ChannelResult::from)
                .toList();
        return new PageResult<>(items, memberships.page(), memberships.pageSize(), memberships.total());
    }

    @Override
    public ChannelResult getChannelById(long accountId, long channelId) {
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
        // 可见性校验与列表保持同一标准：只要不存在成员关系就拒绝，不为系统频道放开
        if (channelMemberRepository.findByRelation(channel.id(), accountId).isEmpty()) {
            throw ProblemException.of(ChannelProblem.READ_FORBIDDEN);
        }
        return ChannelResult.from(channel);
    }

    @Override
    public ChannelResult createChannel(long accountId, String name, String brief) {
        ChannelResult result = transactionRunner.runInTransaction(afterCommit -> {
            // 频道与创建者的成员关系必须同事务写入，否则会出现"频道存在但没有任何成员"的孤儿频道
            long channelId = idGenerator.nextLongId();
            Instant now = timeProvider.nowInstant();
            Channel channel = new Channel(
                channelId,
                name.trim(),
                brief == null ? "" : brief.trim(),
                false,
                now,
                now
            );
            channelRepository.save(channel);
            // 创建者直接落 OWNER 角色，这是后续所有频道治理动作的权限来源
            channelMemberRepository.save(new ChannelMember(idGenerator.nextLongId(), channel.id(), accountId, ChannelMemberRole.OWNER, now));
            // 提交后通知该账号刷新可见频道集合，回滚时不应发出通知
            channelEventNotifier.accountChannelsChanged(afterCommit, List.of(accountId));
            return ChannelResult.from(channel);
        });
        AuditLogs.success("channel.create", accountId, "channel_id", result.channelId());
        return result;
    }

    @Override
    public void deleteChannel(long operatorAccountId, long channelId) {
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            // 操作者必须先是频道成员，再判断其角色，避免把"非成员"与"角色不足"混为一类拒绝
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.of(ChannelProblem.OWNER_REQUIRED);
            }
            // 获取成员关系
            List<Long> members = channelMemberRepository.listByChannelId(channelId).stream()
                .map(ChannelMember::accountId)
                .toList();

            // 删除依赖数据：集合级删除，避免先加载全量申请/禁言再逐条删除
            channelApplicationRepository.deleteByChannelId(channelId);
            channelBanRepository.deleteByChannelId(channelId);
            channelMemberRepository.deleteByChannelId(channel.id());
            channelRepository.delete(channel.id());
            // 通知成员
            channelEventNotifier.accountChannelsChanged(afterCommit, members);
        });
        AuditLogs.success("channel.delete", operatorAccountId, "channel_id", channelId);
    }

    @Override
    public void updateChannelProfile(long operatorAccountId, long channelId, String name, String brief) {
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 资料修改按成员角色放行，不再按频道类型区分；系统频道的成员角色为 MEMBER，因此天然被这一层拦住
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.PROFILE_FORBIDDEN);
            }
            // 领域模型不可变，这里以原主键与原始创建时间重建频道对象；其余未参与本次更新的字段需显式保留
            Channel updated = new Channel(
                channel.id(),
                name.trim(),
                brief == null ? "" : brief.trim(),
                false,
                channel.createdAt(),
                timeProvider.nowInstant()
            );
            channelRepository.update(updated);
            // 资料变化只影响频道本身，因此按频道维度取全体成员作为接收快照
            channelEventNotifier.channelChanged(
                afterCommit,
                updated.id(),
                ChannelChangeScope.PROFILE,
                channelMemberRepository.listByChannelId(channelId).stream().map(ChannelMember::accountId).toList()
            );
            return ChannelResult.from(updated);
        });
        AuditLogs.success("channel.update", operatorAccountId, "channel_id", channelId);
    }

}
