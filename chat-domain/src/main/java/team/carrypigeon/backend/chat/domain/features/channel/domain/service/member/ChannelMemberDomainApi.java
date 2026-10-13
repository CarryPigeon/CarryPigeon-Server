package team.carrypigeon.backend.chat.domain.features.channel.domain.service.member;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道治理领域服务。
 * 职责：承接成员角色治理、踢出和所有权转移等写侧用例。
 * 边界：这里只处理成员治理动作，不承担频道生命周期、禁言和申请流编排。
 */
@Service
public class ChannelMemberDomainApi implements ChannelMemberApi {

    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String CHANNEL_MEMBER_NOT_FOUND_MESSAGE = "channel member does not exist";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final UserProfileApi userProfileApi;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;

    public ChannelMemberDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        UserProfileApi userProfileApi,
        AfterCommitEventPublisher eventPublisher,
        IdGenerator idGenerator,
        TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.userProfileApi = userProfileApi;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query) {
        // 获取频道信息
        Channel channel = channelRepository.findById(query.channelId())
            .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
        // 不能是系统频道
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                "system_channel_members_hidden",
                "system channel member list is not available"
            );
        }
        // 判断是否存在成员信息，只要是成员就能查询
        channelMemberRepository.findByRelation(query.channelId(), query.accountId())
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
        // 查询频道member信息
        List<ChannelMember> members = channelMemberRepository.findMembersByChannelId(channel.id());
        // 查询人员的详细信息
        List<Long> accountIds = members.stream().map(ChannelMember::accountId).toList();
        Map<Long, UserProfileResult> profiles = userProfileApi.getPublicUserProfiles(accountIds).stream()
            .collect(Collectors.toMap(
                UserProfileResult::accountId,
                Function.identity()
            ));

        // 信息整合并返回
        return members.stream()
            .map(member -> ChannelMemberResult.from(member, profiles.get(member.accountId())))
            .toList();
    }

    /**
     * 将普通成员提升为频道管理员。
     * 副作用：更新成员角色、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 成员提升命令
     * @return 提升后的成员投影
     */
    public ChannelMemberResult promoteChannelMember(PromoteChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 获取频道信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            // 获取对应的成员信息
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            // 判断是否为系统频道，系统频道只能在后台管理管理员
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            // 判断操作者是否是管理员
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                    "channel_role_forbidden",
                    "channel action requires owner role"
                );
            }
            // 判断目标用户是否是普通用户
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            // 新的成员信息
            ChannelMember promotedMember = new ChannelMember(
                idGenerator.nextLongId(),
                target.channelId(),
                target.accountId(),
                ChannelMemberRole.ADMIN,
                target.joinedAt()
            );
            // 信息持久化
            channelMemberRepository.update(promotedMember);
            // 审计日志输出
            List<Long> accounts = channelMemberRepository.findMembersByChannelId(command.channelId())
                .stream().map(ChannelMember::accountId).toList();
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(channel.id(), "members", accounts));
            // 组装并返回信息
            return ChannelMemberResult.from(promotedMember, userProfileApi.getUserProfileByAccountId(new GetUserProfileByAccountIdQuery(command.targetAccountId())));
        });
    }

    /**
     * 将频道管理员降级为普通成员。
     * 副作用：更新成员角色、追加审计日志，并在事务提交后通知成员集变化。
     *
     * @param command 管理员降级命令
     * @return 降级后的成员投影
     */
    public ChannelMemberResult demoteChannelAdmin(DemoteChannelAdminCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 获取频道信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            // 获取对应的成员信息
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            // 判断是否为系统频道，系统频道只能在后台管理管理员
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            // 判断操作者是否是管理员
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                    "channel_role_forbidden",
                    "channel action requires owner role"
                );
            }
            // 判断目标用户是否是普通用户
            if (target.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            // 新的成员信息
            ChannelMember promotedMember = new ChannelMember(
                idGenerator.nextLongId(),
                target.channelId(),
                target.accountId(),
                ChannelMemberRole.MEMBER,
                target.joinedAt()
            );
            // 信息持久化
            channelMemberRepository.update(promotedMember);
            // 审计日志输出
            List<Long> accounts = channelMemberRepository.findMembersByChannelId(command.channelId())
                .stream().map(ChannelMember::accountId).toList();
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(channel.id(), "members", accounts));
            // 组装并返回信息
            return ChannelMemberResult.from(promotedMember, userProfileApi.getUserProfileByAccountId(new GetUserProfileByAccountIdQuery(command.targetAccountId())));
        });
    }

    /**
     * 转移频道所有权。
     * 副作用：原所有者降为管理员、目标成员升为所有者，追加审计日志并通知成员集变化。
     *
     * @param command 所有权转移命令
     * @return 所有权转移结果投影
     */
    public ChannelOwnershipTransferResult transferChannelOwnership(TransferChannelOwnershipCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 获取需要的信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
            // 判断是否是系统频道
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            // 判断执行者是否是所有人
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                    "channel_ownership_forbidden",
                    "channel action requires owner role"
                );
            }
            // 目标用户不能是所有人
            if (target.role() == ChannelMemberRole.OWNER) {
                throw ProblemException.validationFailed("target member is already owner");
            }
            // 新信息
            ChannelMember previousOwner = new ChannelMember(
                idGenerator.nextLongId(),
                operator.channelId(),
                operator.accountId(),
                ChannelMemberRole.ADMIN,
                operator.joinedAt()
            );
            ChannelMember newOwner = new ChannelMember(
                idGenerator.nextLongId(),
                target.channelId(),
                target.accountId(),
                ChannelMemberRole.OWNER,
                target.joinedAt()
            );
            // 持久化数据
            channelMemberRepository.update(previousOwner);
            channelMemberRepository.update(newOwner);
            // 后置处理
            List<Long> accounts = channelMemberRepository.findMembersByChannelId(command.channelId())
                .stream().map(ChannelMember::accountId).toList();
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(channel.id(), "members", accounts));
            // 生成返回值
            return ChannelOwnershipTransferResult.from(previousOwner, newOwner);
        });
    }

    /**
     * 将成员踢出频道。
     * 副作用：删除成员关系、追加审计日志，并在事务提交后通知频道成员集和目标账号频道集变化。
     *
     * @param command 踢出成员命令
     */
    public void kickChannelMember(KickChannelMemberCommand command) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 查询相关信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
            // 判断是否是系统频道
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            // 执行者必须有权限
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_kick_forbidden",
                    "channel action requires owner or admin role"
                );
            }
            // 不能踢走管理或者群主
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            channelMemberRepository.delete(target.memberId());
            List<Long> accounts = channelMemberRepository.findMembersByChannelId(command.channelId())
                .stream().map(ChannelMember::accountId).toList();
            // 通知其他人频道列表改变
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new ChannelChangedEvent(channel.id(), "members", accounts));
            // 通知目标成员你被提了
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(target.accountId()));
        });
    }

}
