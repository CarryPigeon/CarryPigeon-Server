package team.carrypigeon.backend.chat.domain.features.channel.domain.service.ban;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.ChannelBanApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道禁言领域 API 实现。
 * 职责：独立承接禁言、限时禁言和解除禁言，不依赖成员治理 API。
 */
@Service
public class ChannelBanDomainApi implements ChannelBanApi {

    private static final String CHANNEL_BAN_NOT_FOUND_MESSAGE = "channel ban does not exist";
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;

    public ChannelBanDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        ChannelBanRepository channelBanRepository,
        AfterCommitEventPublisher eventPublisher,
        TimeProvider timeProvider,
        TransactionRunner transactionRunner,
        IdGenerator idGenerator
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query) {
        // 查询基础信息
        Channel channel = channelRepository.findById(query.channelId())
            .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        ChannelMember operator = channelMemberRepository.findByRelation(query.channelId(), query.accountId())
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        // TODO 系统频道不参与治理
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                "regular_channel_required",
                "channel invite flow is not supported for system channels"
            );
        }
        // 权限校验
        if (operator.role() != ChannelMemberRole.OWNER
            && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(
                "channel_ban_forbidden",
                "channel action requires owner or admin role"
            );
        }
        // 数据转换
        return channelBanRepository.list(channel.id()).stream()
            .map(ChannelBanListItemResult::from)
            .toList();
    }

    @Override
    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询基本信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            // 判断不是系统频道
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            // 判断是否有权限
            if (operator.role() != ChannelMemberRole.OWNER
                && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_ban_forbidden",
                    "channel action requires owner or admin role"
                );
            }
            if (target.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            // 查询已有信息
            ChannelBan existingBan = channelBanRepository.findByRelation(
                channel.id(), target.accountId()).orElse(null);
            if (existingBan != null
                && existingBan.revokedAt() == null
                && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is already active");
            }
            ChannelBan ban;
            String reason = command.reason() == null || command.reason().isBlank() ? "" : command.reason().trim();
            Instant now = timeProvider.nowInstant();
            if (existingBan == null) {
                ban = new ChannelBan(
                    idGenerator.nextLongId(),
                    channel.id(),
                    target.accountId(),
                    operator.accountId(),
                    reason,
                    command.durationSeconds() == null ? null : now.plusSeconds(command.durationSeconds()),
                    now, null
                );
                channelBanRepository.save(ban);
            } else {
                ban = new ChannelBan(
                    existingBan.banId(),
                    channel.id(),
                    target.accountId(),
                    operator.accountId(),
                    reason,
                    command.durationSeconds() == null ? null : now.plusSeconds(command.durationSeconds()),
                    now, null
                );
                channelBanRepository.update(ban);
            }
            List<Long> recipients = channelMemberRepository.listByChannelId(channel.id()).stream().map(ChannelMember::accountId).toList();
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new ChannelChangedEvent(channel.id(), "bans", recipients));
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(target.accountId()));
            return ChannelBanResult.from(ban);
        });
    }

    @Override
    public ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command) {
        Long durationSeconds = null;
        if (command.untilEpochMillis() != null) {
            long nowMillis = timeProvider.nowMillis();
            if (command.untilEpochMillis() <= nowMillis) {
                throw ProblemException.validationFailed("until must be greater than current time");
            }
            durationSeconds = Math.max(1L, (command.untilEpochMillis() - nowMillis) / 1000L);
        }
        return banChannelMember(new BanChannelMemberCommand(
            command.operatorAccountId(), command.channelId(), command.targetAccountId(),
            command.reason(), durationSeconds));
    }

    @Override
    public ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询基础信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            // 判断权限与可操作性
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER
                && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_ban_forbidden",
                    "channel action requires owner or admin role"
                );
            }
            ChannelBan existingBan = channelBanRepository.findByRelation(
                    channel.id(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_BAN_NOT_FOUND_MESSAGE));
            if (existingBan.revokedAt() != null
                || (existingBan.expiresAt() != null && !existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is not active");
            }
            ChannelBan revokedBan = new ChannelBan(
                existingBan.banId(),
                existingBan.channelId(), existingBan.bannedAccountId(), operator.accountId(),
                existingBan.reason(), existingBan.expiresAt(), existingBan.createdAt(), timeProvider.nowInstant());
            // 更新数据
            channelBanRepository.update(revokedBan);
            // 后置处理
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(
                    channel.id(), "bans", channelMemberRepository.listByChannelId(channel.id()).stream().map(ChannelMember::accountId).toList()));
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(command.targetAccountId()));
            // 响应数据
            return ChannelBanResult.from(revokedBan);
        });
    }

}
