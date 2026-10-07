package team.carrypigeon.backend.chat.domain.features.channel.domain.service.ban;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;

import java.time.Instant;
import java.util.List;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.ChannelBanApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
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
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelBanDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            AfterCommitEventPublisher eventPublisher,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.afterCommitEventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        if (channel.systemChannel()) {
            throw ProblemException.forbidden(
                    "regular_channel_required",
                    "channel invite flow is not supported for system channels"
            );
        }
        if (operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.OWNER
                && operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(
                    "channel_ban_forbidden",
                    "channel action requires owner or admin role"
            );
        }
        return channelBanRepository.findByChannelId(channel.id()).stream()
                .map(ChannelBanListItemResult::from)
                .toList();
    }

    @Override
    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId());
        if (command.durationSeconds() != null && command.durationSeconds() <= 0) {
            throw ProblemException.validationFailed("durationSeconds must be greater than 0");
        }
        if (command.reason() != null && command.reason().trim().length() > 256) {
            throw ProblemException.validationFailed("reason length must be less than or equal to 256");
        }
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            ChannelMember target = requireMember(channel.id(), command.targetAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.OWNER
                    && operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_ban_forbidden",
                        "channel action requires owner or admin role"
                );
            }
            if (target.role() == team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.OWNER) {
                throw ProblemException.forbidden(
                        "channel_ban_forbidden",
                        "target member with OWNER role cannot be moderated"
                );
            }
            if (target.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            ChannelBan existingBan = channelBanRepository.findByChannelIdAndBannedAccountId(
                    channel.id(), target.accountId()).orElse(null);
            if (existingBan != null
                    && existingBan.revokedAt() == null
                    && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is already active");
            }
            Instant now = timeProvider.nowInstant();
            ChannelBan ban = new ChannelBan(
                    channel.id(), target.accountId(), operator.accountId(),
                    normalizeReason(command.reason()),
                    command.durationSeconds() == null ? null : now.plusSeconds(command.durationSeconds()),
                    now, null
            );
            if (existingBan == null) {
                channelBanRepository.save(ban);
            } else {
                channelBanRepository.update(ban);
            }
            List<Long> recipients = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_BANNED", target.accountId(),
                    buildBanAuditMetadata(ban));
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
        validateTargetedCommand(command.operatorAccountId(), command.channelId(), command.targetAccountId());
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMember(channel.id(), command.operatorAccountId());
            if (channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "regular_channel_required",
                        "channel invite flow is not supported for system channels"
                );
            }
            if (operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.OWNER
                    && operator.role() != team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_ban_forbidden",
                        "channel action requires owner or admin role"
                );
            }
            ChannelBan existingBan = channelBanRepository.findByChannelIdAndBannedAccountId(
                    channel.id(), command.targetAccountId())
                    .orElseThrow(() -> ProblemException.notFound(CHANNEL_BAN_NOT_FOUND_MESSAGE));
            if (existingBan.revokedAt() != null
                    || (existingBan.expiresAt() != null && !existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is not active");
            }
            ChannelBan revokedBan = new ChannelBan(
                    existingBan.channelId(), existingBan.bannedAccountId(), operator.accountId(),
                    existingBan.reason(), existingBan.expiresAt(), existingBan.createdAt(), timeProvider.nowInstant());
            channelBanRepository.update(revokedBan);
            appendAuditLog(channel.id(), operator.accountId(), "MEMBER_UNBANNED",
                    command.targetAccountId(), null);
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute,
                    new ChannelChangedEvent(
                            channel.id(), "bans", channelMemberRepository.findAccountIdsByChannelId(channel.id())));
            afterCommitEventPublisher.publishAfterCommit(
                    afterCommit::execute, new AccountChannelsChangedEvent(command.targetAccountId()));
            return ChannelBanResult.from(revokedBan);
        });
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
    }

    private void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId) {
        requirePositive(operatorAccountId, "operatorAccountId");
        requirePositive(channelId, "channelId");
        requirePositive(targetAccountId, "targetAccountId");
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal targetAccountId");
        }
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "" : reason.trim();
    }

    private String buildBanAuditMetadata(ChannelBan ban) {
        StringBuilder metadata = new StringBuilder("{\"expiresAt\":");
        if (ban.expiresAt() == null) {
            metadata.append("null");
        } else {
            metadata.append("\"").append(ban.expiresAt()).append("\"");
        }
        return metadata.append('}').toString();
    }

    private void appendAuditLog(long channelId, long actorAccountId, String action, long targetAccountId, String metadata) {
        channelAuditLogRepository.append(new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog(
                0L, channelId, actorAccountId, action, targetAccountId, metadata, timeProvider.nowInstant()));
    }

}
