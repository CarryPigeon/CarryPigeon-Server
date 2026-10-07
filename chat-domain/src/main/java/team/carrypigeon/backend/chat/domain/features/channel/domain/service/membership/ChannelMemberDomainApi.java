package team.carrypigeon.backend.chat.domain.features.channel.domain.service.membership;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.ChannelMemberApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.input.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
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
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final UserProfileApi userProfileApi;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelMemberDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            UserProfileApi userProfileApi,
            AfterCommitEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.userProfileApi = userProfileApi;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                    "system_channel_members_hidden",
                    "system channel member list is not available"
            );
        }
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        List<ChannelMember> members = channelMemberRepository.findByChannelId(channel.id());
        Map<Long, UserProfileResult> profiles = findPublicProfiles(members);
        return members.stream()
                .map(member -> {
                    UserProfileResult profile = profiles.get(member.accountId());
                    return ChannelMemberResult.from(
                            member,
                            profile == null ? "" : profile.username(),
                            profile == null ? "" : profile.avatarUrl()
                    );
                })
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
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                        "channel_role_forbidden",
                        "channel action requires owner role"
                );
            }
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            ChannelMember promotedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.ADMIN,
                    target.joinedAt()
            );
            channelMemberRepository.update(promotedMember);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_PROMOTED_TO_ADMIN", target.accountId(), null);
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "members", snapshotChannelRecipientAccountIds(channel.id())));
            UserProfileResult profile = findPublicProfile(promotedMember);
            return ChannelMemberResult.from(promotedMember, profile.username(), profile.avatarUrl());
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
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                        "channel_role_forbidden",
                        "channel action requires owner role"
                );
            }
            if (target.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.validationFailed("target member must have ADMIN role");
            }
            ChannelMember demotedMember = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.MEMBER,
                    target.joinedAt()
            );
            channelMemberRepository.update(demotedMember);
            appendAuditLog(channel.id(), operator.accountId(), "ADMIN_DEMOTED_TO_MEMBER", target.accountId(), null);
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "members", snapshotChannelRecipientAccountIds(channel.id())));
            UserProfileResult profile = findPublicProfile(demotedMember);
            return ChannelMemberResult.from(demotedMember, profile.username(), profile.avatarUrl());
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
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                        "channel_ownership_forbidden",
                        "channel action requires owner role"
                );
            }
            if (target.role() == ChannelMemberRole.OWNER) {
                throw ProblemException.validationFailed("target member is already owner");
            }
            ChannelMember previousOwner = new ChannelMember(
                    operator.channelId(),
                    operator.accountId(),
                    ChannelMemberRole.ADMIN,
                    operator.joinedAt()
            );
            ChannelMember newOwner = new ChannelMember(
                    target.channelId(),
                    target.accountId(),
                    ChannelMemberRole.OWNER,
                    target.joinedAt()
            );
            if (!channelMemberRepository.transferOwnership(channel.id(), operator.accountId(), target.accountId())) {
                throw ProblemException.conflict("ownership_transfer_conflict", "channel ownership changed concurrently");
            }
            appendAuditLog(
                    channel.id(),
                    operator.accountId(),
                    "CHANNEL_OWNERSHIP_TRANSFERRED",
                    target.accountId(),
                    "{\"previousOwnerAccountId\":" + operator.accountId() + "}"
            );
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(channel.id(), "members", snapshotChannelRecipientAccountIds(channel.id())));
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
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId(), "targetAccountId");
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireTargetMember(channel.id(), command.targetAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_kick_forbidden",
                        "channel action requires owner or admin role"
                );
            }
            if (target.role() == ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                        "channel_kick_forbidden",
                        "target member with OWNER role cannot be moderated"
                );
            }
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            List<Long> recipientAccountIds = snapshotChannelRecipientAccountIds(channel.id());
            channelMemberRepository.delete(channel.id(), target.accountId());
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_KICKED", target.accountId(), null);
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new ChannelChangedEvent(channel.id(), "members", recipientAccountIds));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(target.accountId()));
            return null;
        });
    }

    private void validateTargetedCommand(
            long operatorAccountId,
            long channelId,
            long targetAccountId,
            String targetFieldName
    ) {
        requirePositive(operatorAccountId, "operatorAccountId");
        requirePositive(channelId, "channelId");
        requirePositive(targetAccountId, targetFieldName);
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal " + targetFieldName);
        }
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private ChannelMember requireTargetMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
    }

    private void appendAuditLog(
            long channelId,
            long actorAccountId,
            String actionType,
            Long targetAccountId,
            String metadata
    ) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                idGenerator.nextLongId(),
                channelId,
                actorAccountId,
                actionType,
                targetAccountId,
                metadata == null ? "{}" : metadata,
                timeProvider.nowInstant()
        ));
    }

    private List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

    private UserProfileResult findPublicProfile(ChannelMember member) {
        return findPublicProfiles(List.of(member)).get(member.accountId());
    }

    private Map<Long, UserProfileResult> findPublicProfiles(List<ChannelMember> members) {
        if (members.isEmpty()) {
            return Map.of();
        }
        List<Long> accountIds = members.stream().map(ChannelMember::accountId).toList();
        return userProfileApi.getPublicUserProfiles(accountIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserProfileResult::accountId,
                        java.util.function.Function.identity(),
                        (left, right) -> left
                ));
    }

}
