package team.carrypigeon.backend.chat.domain.features.channel.domain.service.application;


import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplicationStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道申请流领域服务。
 * 职责：承接成员邀请、入群申请、审批与接受邀请等流程编排。
 * 边界：这里只处理 invite/application 生命周期，不承担频道目录查询或成员治理。
 */
@Service
public class ChannelApplicationDomainApi implements ChannelApplicationApi {

    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelApplicationRepository channelInviteRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelApplicationDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelApplicationRepository channelInviteRepository,
            AfterCommitEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ChannelApplicationResult createChannelApplication(CreateChannelApplicationCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询频道信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
            // 拒绝系统频道
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel application flow is not supported for system channels"
                );
            }
            // 拒绝已有成员
            if (channelMemberRepository.findByRelation(channel.id(), command.accountId()).isPresent()) {
                throw ProblemException.validationFailed("current account is already a channel member");
            }
            ChannelApplication existingApplication = channelInviteRepository.findByChannelAndApplicant(channel.id(), command.accountId())
                    .orElse(null);
            if (existingApplication != null && existingApplication.status() == ChannelApplicationStatus.PENDING) {
                throw ProblemException.validationFailed("pending channel application already exists");
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
            ChannelApplication persistedInvite = null;
            if (existingApplication == null) {
                channelInviteRepository.save(application);
            } else {
                persistedInvite = new ChannelApplication(
                    existingApplication.applicationId(),
                    existingApplication.applicantId(),
                    existingApplication.channelId(),
                    reason,
                    ChannelApplicationStatus.PENDING,
                    timeProvider.nowInstant(),
                    null
                );
                channelInviteRepository.update(persistedInvite);
            }
            // 后置处理
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "applications", channelMemberRepository.findMembersByChannelId(command.channelId()).stream().map(ChannelMember::accountId).toList()));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(command.accountId()));
            // 响应
            return ChannelApplicationResult.from(Objects.requireNonNull(persistedInvite), reason);
        });
    }

    /**
     * 查询频道入群申请列表。
     * 约束：调用方必须具备邀请或审核申请的频道权限。
     *
     * @param query 入群申请列表查询
     * @return 入群申请投影列表
     */
    @Override
    public List<ChannelApplicationResult> listChannelApplications(ListChannelApplicationsQuery query) {
        // 查询基础信息
        Channel channel = channelRepository.findById(query.channelId())
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
        ChannelMember operator = channelMemberRepository.findByRelation(query.channelId(), query.accountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
        // 校验
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
            );
        }
        if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(
                    "channel_invite_forbidden",
                    "channel invite requires owner or admin role"
            );
        }
        // 组装数据
        return channelInviteRepository.findByChannelId(channel.id()).stream()
                .map(invite -> ChannelApplicationResult.from(
                        invite,
                        invite.reason() == null || invite.reason().isBlank() ? "" : invite.reason().trim()
                ))
                .toList();
    }

    /**
     * 审批频道入群申请。
     * 副作用：更新申请状态；批准时新增频道成员，并在事务提交后通知申请集、成员集和账号频道集变化。
     *
     * @param command 入群申请审批命令
     * @return 审批后的申请投影
     */
    @Override
    public ChannelApplicationResult decideChannelApplication(DecideChannelApplicationCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询申请
            ChannelApplication application = channelInviteRepository.findById(command.applicationId())
                    .orElseThrow(() -> ProblemException.notFound("channel application does not exist"));
            // 必须没有处理过
            if (application.status() != ChannelApplicationStatus.PENDING) {
                throw ProblemException.conflict("application_already_processed", "channel application is already processed");
            }
            // 查询基础信息
            ChannelMember operator = channelMemberRepository.findByRelation(application.channelId(),command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
            // 权限判断和频道判断
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_invite_forbidden",
                    "channel application requires owner or admin role"
                );
            }
            // 创建处理实体类
            ChannelApplicationStatus decidedStatus = switch (command.decision().trim().toLowerCase()) {
                case "approve" -> ChannelApplicationStatus.ACCEPTED;
                case "reject" -> ChannelApplicationStatus.DECLINED;
                default -> throw ProblemException.validationFailed("decision must be approve or reject");
            };
            // 判断不是成员
            if(channelMemberRepository.findByRelation(application.channelId(), application.applicantId()).isPresent()){
                throw ProblemException.conflict("already a member");
            }
            // 更新数据
            ChannelApplication updated = new ChannelApplication(
                application.applicationId(),
                application.applicantId(),
                application.channelId(),
                application.reason(),
                decidedStatus,
                application.createdAt(),
                timeProvider.nowInstant()
            );
            channelInviteRepository.update(updated);
            if (decidedStatus == ChannelApplicationStatus.ACCEPTED) {
                channelMemberRepository.save(new ChannelMember(idGenerator.nextLongId(), application.channelId(), application.applicantId(), ChannelMemberRole.MEMBER, timeProvider.nowInstant()));
            }
            // 后置通知
            List<Long> recipientAccountIds = List.copyOf(channelMemberRepository.findMembersByChannelId(application.channelId()).stream().map(ChannelMember::accountId).toList());
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new ChannelChangedEvent(application.channelId(), "applications", recipientAccountIds));
            if (decidedStatus == ChannelApplicationStatus.ACCEPTED) {
                afterCommitEventPublisher.publishAfterCommit(
                        afterCommit::execute, new ChannelChangedEvent(application.channelId(), "members", recipientAccountIds));
            }
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(application.applicantId()));
            return ChannelApplicationResult.from(
                    updated,
                    updated.reason() == null || updated.reason().isBlank() ? "" : updated.reason().trim()
            );
        });
    }

}
