package team.carrypigeon.backend.chat.domain.features.channel.domain.service.application;


import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelChangeScope;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplicationStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道申请流领域服务。
 * 职责：承接成员邀请、入群申请、审批与接受邀请等流程编排。
 * 边界：这里只处理 invite/application 生命周期，不承担频道目录查询或成员治理。
 */
@Service
public class ChannelApplicationDomainApi implements ChannelApplicationApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelApplicationRepository channelApplicationRepository;
    private final ChannelEventNotifier channelEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelApplicationRepository channelInviteRepository,
            ChannelEventNotifier channelEventNotifier,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelApplicationRepository = channelInviteRepository;
        this.channelEventNotifier = channelEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 创建频道入群申请。
     * 输入：命令携带申请账号、目标频道与申请理由。
     * 输出：新建申请的投影。
     * 副作用：写入一条 PENDING 申请，并替换该账号在同一频道的历史申请记录。
     * 约束：申请账号不能已是频道成员；同频道已存在 PENDING 申请时拒绝重复提交。
     *
     * @param command 频道入群申请创建命令
     * @return 创建后的频道申请投影
     */
    @Override
    public ChannelApplicationResult createChannelApplication(CreateChannelApplicationCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            // 已是成员时无需申请，直接按参数错误拒绝
            if (channelMemberRepository.findByRelation(channel.id(), command.accountId()).isPresent()) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "current account is already a channel member");
            }
            // 同一 (频道, 申请人) 至多保留一条申请：已审批的历史记录会被本次申请覆盖
            ChannelApplication existingApplication = channelApplicationRepository.findByRelation(channel.id(), command.accountId())
                    .orElse(null);
            if (existingApplication != null && existingApplication.status() == ChannelApplicationStatus.PENDING) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "pending channel application already exists");
            }
            if (existingApplication != null) {
                // 先删旧记录再写入新记录，保证替换过程与下方写入处于同一事务，不会留下两行
                channelApplicationRepository.delete(existingApplication.applicationId());
            }
            String reason = command.reason() == null || command.reason().isBlank() ? "" : command.reason().trim();
            ChannelApplication application = new ChannelApplication(
                idGenerator.nextLongId(),
                command.accountId(),
                channel.id(),
                reason,
                ChannelApplicationStatus.PENDING,
                timeProvider.nowInstant(),
                null
            );
            channelApplicationRepository.save(application);
            // 申请变化只对频道治理者有意义，因此按频道维度取现有成员作为接收快照
            channelEventNotifier.channelChanged(
                    afterCommit,
                    channel.id(),
                    ChannelChangeScope.APPLICATIONS,
                    channelMemberRepository.listByChannelId(command.channelId()).stream().map(ChannelMember::accountId).toList());
            // 申请人自身也需要刷新频道集合，其视角不属于上面的频道成员快照
            channelEventNotifier.accountChannelsChanged(afterCommit, command.accountId());
            return ChannelApplicationResult.from(application, reason);
        });
    }

    /**
     * 查询频道入群申请列表。
     * 输入：查询对象携带请求者账号与目标频道。
     * 输出：该频道全部申请记录的投影列表。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色；申请理由为空时统一返回空字符串而非 null。
     *
     * @param query 入群申请列表查询
     * @return 入群申请投影列表
     */
    @Override
    public List<ChannelApplicationResult> listChannelApplications(ListChannelApplicationsQuery query) {
        Channel channel = channelRepository.findById(query.channelId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
        ChannelMember operator = channelMemberRepository.findByRelation(query.channelId(), query.accountId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
        // 申请名单属于治理数据，普通成员不可读取
        if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.of(ChannelProblem.APPLICATION_LIST_FORBIDDEN);
        }
        // 申请理由在持久化层允许为 null，这里统一归一为空字符串后再暴露给协议层
        return channelApplicationRepository.list(channel.id()).stream()
                .map(invite -> ChannelApplicationResult.from(
                        invite,
                        invite.reason() == null || invite.reason().isBlank() ? "" : invite.reason().trim()
                ))
                .toList();
    }

    /**
     * 审批频道入群申请。
     * 输入：命令携带审批操作者、申请所属频道、申请 ID 与审批决定。
     * 输出：审批后的申请投影。
     * 副作用：更新申请状态与响应时间；批准时新增一条 MEMBER 成员关系，并在事务提交后通知申请集、成员集与申请人频道集合变化。
     * 约束：申请必须归属于命令声明的频道且处于 PENDING 状态；审批人必须具备该频道 OWNER 或 ADMIN 角色；申请人不能已是成员。
     *
     * @param command 入群申请审批命令
     * @return 审批后的申请投影
     */
    @Override
    public ChannelApplicationResult decideChannelApplication(DecideChannelApplicationCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            ChannelApplication application = channelApplicationRepository.findById(command.applicationId())
                    .orElseThrow(() -> ProblemException.of(ChannelProblem.APPLICATION_NOT_FOUND));
            // 归属校验：路径声明的频道必须与申请实际所属频道一致，否则按不存在处理以免泄露其它频道的申请存在性
            if (application.channelId() != command.channelId()) {
                throw ProblemException.of(ChannelProblem.APPLICATION_NOT_FOUND);
            }
            // 幂等保护：只有 PENDING 申请可被审批，重复审批按冲突拒绝
            if (application.status() != ChannelApplicationStatus.PENDING) {
                throw ProblemException.of(ChannelProblem.APPLICATION_ALREADY_PROCESSED);
            }
            ChannelMember operator = channelMemberRepository.findByRelation(application.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 审批属于治理动作，普通成员不可执行
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.APPLICATION_DECIDE_FORBIDDEN);
            }
            // 协议层只传字符串决定，这里收敛为固定枚举，未知取值按参数错误拒绝
            ChannelApplicationStatus decidedStatus = switch (command.decision().trim().toLowerCase()) {
                case "approve" -> ChannelApplicationStatus.ACCEPTED;
                case "reject" -> ChannelApplicationStatus.DECLINED;
                default -> throw ProblemException.of(ChannelProblem.INVALID_INPUT, "decision must be approve or reject");
            };
            // 申请人可能在申请期间已通过其它途径入群，此时审批失去意义
            if (channelMemberRepository.findByRelation(application.channelId(), application.applicantId()).isPresent()) {
                throw ProblemException.of(ChannelProblem.APPLICANT_ALREADY_MEMBER);
            }
            // 领域模型不可变：沿用原申请主键与创建时间，只替换状态并写入响应时间
            ChannelApplication updated = new ChannelApplication(
                application.applicationId(),
                application.applicantId(),
                application.channelId(),
                application.reason(),
                decidedStatus,
                application.createdAt(),
                timeProvider.nowInstant()
            );
            channelApplicationRepository.update(updated);
            if (decidedStatus == ChannelApplicationStatus.ACCEPTED) {
                // 批准即建立成员关系，初始角色固定为 MEMBER；申请状态与成员写入必须同事务
                channelMemberRepository.save(new ChannelMember(idGenerator.nextLongId(), application.channelId(), application.applicantId(), ChannelMemberRole.MEMBER, timeProvider.nowInstant()));
            }
            // 在事务内取成员快照：批准后申请人已在其中，拒绝时则不含申请人
            List<Long> recipientAccountIds = List.copyOf(channelMemberRepository.listByChannelId(application.channelId()).stream().map(ChannelMember::accountId).toList());
            channelEventNotifier.channelChanged(
                    afterCommit, application.channelId(), ChannelChangeScope.APPLICATIONS, recipientAccountIds);
            if (decidedStatus == ChannelApplicationStatus.ACCEPTED) {
                // 仅批准会改变成员集，拒绝不需要发出成员集变化通知
                channelEventNotifier.channelChanged(
                        afterCommit, application.channelId(), ChannelChangeScope.MEMBERS, recipientAccountIds);
            }
            // 申请人视角的频道集合也需要刷新，与上面的频道成员快照相互独立
            channelEventNotifier.accountChannelsChanged(afterCommit, application.applicantId());
            return ChannelApplicationResult.from(
                    updated,
                    updated.reason() == null || updated.reason().isBlank() ? "" : updated.reason().trim()
            );
        });
    }

}
