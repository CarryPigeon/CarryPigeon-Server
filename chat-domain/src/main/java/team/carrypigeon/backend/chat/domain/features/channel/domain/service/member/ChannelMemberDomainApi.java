package team.carrypigeon.backend.chat.domain.features.channel.domain.service.member;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.MemberProfileQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelChangeScope;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.MemberProfile;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 频道治理领域服务。
 * 职责：承接成员角色治理、踢出和所有权转移等写侧用例。
 * 边界：这里只处理成员治理动作，不承担频道生命周期、禁言和申请流编排。
 */
@Service
public class ChannelMemberDomainApi implements ChannelMemberApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelEventNotifier channelEventNotifier;
    private final MemberProfileQuery memberProfileQuery;
    private final TransactionRunner transactionRunner;

    public ChannelMemberDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        MemberProfileQuery memberProfileQuery,
        ChannelEventNotifier channelEventNotifier,
        TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelEventNotifier = channelEventNotifier;
        this.memberProfileQuery = memberProfileQuery;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<ChannelMemberResult> listChannelMembers(long accountId, long channelId) {
        // 先确认频道存在，避免把"频道不存在"与"不是成员"混为一类拒绝
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
        // 读取权限下放到成员身份：只要请求者本人是成员，就允许查看完整成员列表
        channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
        List<ChannelMember> members = channelMemberRepository.listByChannelId(channel.id());
        // 一次性批量取回全部成员资料并按账号建索引，避免在下方映射时逐个查询
        List<Long> accountIds = members.stream().map(ChannelMember::accountId).toList();
        Map<Long, MemberProfile> profiles = memberProfileQuery.findProfiles(accountIds);

        // 成员资料缺失时对应投影字段为空，这里不补默认值，由协议层决定如何展示
        return members.stream()
            .map(member -> ChannelMemberResult.from(member, profiles.get(member.accountId())))
            .toList();
    }

    @Override
    public void promoteChannelMember(long operatorAccountId, long channelId, long targetAccountId) {
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            // 操作者与目标都必须先是频道成员，否则无法进入角色判定
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                    .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            ChannelMember target = channelMemberRepository.findByRelation(channelId, targetAccountId)
                    .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 角色调整统一只认 OWNER，ADMIN 不能自行扩张管理员数量
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.of(ChannelProblem.ROLE_FORBIDDEN);
            }
            // 只允许 MEMBER 升为 ADMIN，避免重复提升或跨级覆盖 OWNER
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "target member must have MEMBER role");
            }
            // 领域模型不可变：沿用目标成员原主键与原始加入时间，只替换角色
            ChannelMember promotedMember = new ChannelMember(
                    target.id(),
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.ADMIN,
                    target.joinedAt()
            );
            channelMemberRepository.update(promotedMember);
            // 在事务内取成员账号快照，保证通知的接收人集合与本次角色变更一致
            List<Long> accounts = channelMemberRepository.listByChannelId(channelId)
                    .stream().map(ChannelMember::accountId).toList();
            channelEventNotifier.channelChanged(
                    afterCommit, channel.id(), ChannelChangeScope.MEMBERS, accounts);
            return ChannelMemberResult.from(promotedMember, memberProfileQuery.findProfile(targetAccountId));
        });
    }

    @Override
    public void demoteChannelAdmin(long operatorAccountId, long channelId, long targetAccountId) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 获取频道信息
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            // 操作者与目标都必须先是频道成员，否则无法进入角色判定
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            ChannelMember target = channelMemberRepository.findByRelation(channelId, targetAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 角色调整统一只认 OWNER，ADMIN 不能撤销其它管理员
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.of(ChannelProblem.ROLE_FORBIDDEN);
            }
            // 只允许 ADMIN 降为 MEMBER；对已是 MEMBER 的目标重复调用视为参数错误
            // NOTE: 此处错误文案当前仍为 "target member must have MEMBER role"，与实际判定条件（需为 ADMIN）不符，待你确认后再单独修正
            if (target.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "target member must have MEMBER role");
            }
            // 领域模型不可变：沿用目标成员原主键与原始加入时间，只把角色改回 MEMBER
            ChannelMember demotedMember = new ChannelMember(
                target.id(),
                target.channelId(),
                target.accountId(),
                ChannelMemberRole.MEMBER,
                target.joinedAt()
            );
            channelMemberRepository.update(demotedMember);
            // 在事务内取成员账号快照，保证通知的接收人集合与本次角色变更一致
            List<Long> accounts = channelMemberRepository.listByChannelId(channelId)
                .stream().map(ChannelMember::accountId).toList();
            channelEventNotifier.channelChanged(
                afterCommit, channel.id(), ChannelChangeScope.MEMBERS, accounts);
            return ChannelMemberResult.from(demotedMember, memberProfileQuery.findProfile(targetAccountId));
        });
    }

    @Override
    public ChannelOwnershipTransferResult transferChannelOwnership(long operatorAccountId, long channelId, long targetAccountId) {
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 目标不是成员时按资源不存在处理，与操作者非成员的拒绝语义区分开
            ChannelMember target = channelMemberRepository.findByRelation(channelId, targetAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBER_NOT_FOUND));
            // 只有现任 OWNER 才能发起转移，避免 ADMIN 自行接管频道
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.of(ChannelProblem.OWNERSHIP_FORBIDDEN);
            }
            // 目标已是 OWNER 时转移无意义，按参数错误拒绝
            if (target.role() == ChannelMemberRole.OWNER) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "target member is already owner");
            }
            // 两次角色变更必须构成同一事务：否则可能出现频道短暂没有 OWNER 的状态
            // 领域模型不可变，两处都以各自原主键重建；主键必须沿用，否则更新命中不到任何行
            ChannelMember previousOwner = new ChannelMember(
                operator.id(),
                operator.channelId(),
                operator.accountId(),
                ChannelMemberRole.ADMIN,
                operator.joinedAt()
            );
            ChannelMember newOwner = new ChannelMember(
                target.id(),
                target.channelId(),
                target.accountId(),
                ChannelMemberRole.OWNER,
                target.joinedAt()
            );
            channelMemberRepository.update(previousOwner);
            channelMemberRepository.update(newOwner);
            // 先降原 OWNER 再升新 OWNER，顺序保证任意中间点都只有一个 OWNER
            List<Long> accounts = channelMemberRepository.listByChannelId(channelId)
                .stream().map(ChannelMember::accountId).toList();
            channelEventNotifier.channelChanged(
                afterCommit, channel.id(), ChannelChangeScope.MEMBERS, accounts);
            return ChannelOwnershipTransferResult.from(previousOwner, newOwner);
        });
    }

    @Override
    public void kickChannelMember(long operatorAccountId, long channelId, long targetAccountId) {
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            ChannelMember target = channelMemberRepository.findByRelation(channelId, targetAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBER_NOT_FOUND));
            // 治理动作要求 OWNER 或 ADMIN，普通成员不能移除他人
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.KICK_FORBIDDEN);
            }
            // 只允许移除 MEMBER，防止越权踢掉管理员或频道所有者
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "target member must have MEMBER role");
            }
            // 删除成员关系后重新取快照，通知中不再包含被移除者
            channelMemberRepository.delete(target.id());
            List<Long> accounts = channelMemberRepository.listByChannelId(channelId)
                .stream().map(ChannelMember::accountId).toList();
            // 通知频道内其余成员刷新成员列表
            channelEventNotifier.channelChanged(
                afterCommit, channel.id(), ChannelChangeScope.MEMBERS, accounts);
            // 单独通知被移除账号刷新自己的可见频道集合，它已不在上面的接收快照中
            channelEventNotifier.accountChannelsChanged(afterCommit, List.of(target.accountId()));
        });
    }

}
