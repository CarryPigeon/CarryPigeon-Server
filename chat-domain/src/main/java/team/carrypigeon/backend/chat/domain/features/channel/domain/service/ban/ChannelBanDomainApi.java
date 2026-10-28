package team.carrypigeon.backend.chat.domain.features.channel.domain.service.ban;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.ChannelBanApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelChangeScope;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道禁言领域 API 实现。
 * 职责：独立承接禁言、限时禁言和解除禁言，不依赖成员治理 API。
 */
@Service
public class ChannelBanDomainApi implements ChannelBanApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelEventNotifier channelEventNotifier;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;

    public ChannelBanDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        ChannelBanRepository channelBanRepository,
        ChannelEventNotifier channelEventNotifier,
        TimeProvider timeProvider,
        TransactionRunner transactionRunner,
        IdGenerator idGenerator
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelEventNotifier = channelEventNotifier;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<ChannelBanListItemResult> listChannelBans(long accountId, long channelId) {
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
        ChannelMember operator = channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
        // 禁言台账属于治理数据，普通成员不可读取
        if (operator.role() != ChannelMemberRole.OWNER
            && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.of(ChannelProblem.BAN_FORBIDDEN);
        }
        // 这里不过滤 revokedAt 与 expiresAt，由调用方按需判定记录是否仍然生效
        return channelBanRepository.list(channel.id()).stream()
            .map(ChannelBanListItemResult::from)
            .toList();
    }

    @Override
    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 禁言属于治理动作，普通成员不可执行
            if (operator.role() != ChannelMemberRole.OWNER
                && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.BAN_FORBIDDEN);
            }
            // 只允许禁言 MEMBER，避免管理员之间互相禁言
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "target member must have MEMBER role");
            }
            ChannelBan existingBan = channelBanRepository.findByRelation(
                channel.id(), target.accountId()).orElse(null);
            // 已有记录未解除且未到期时属于重复禁言，按参数错误拒绝
            if (existingBan != null
                && existingBan.revokedAt() == null
                && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "channel ban is already active");
            }
            ChannelBan ban;
            String reason = command.reason() == null || command.reason().isBlank() ? "" : command.reason().trim();
            Instant now = timeProvider.nowInstant();
            if (existingBan == null) {
                // 首次禁言：发新主键并插入
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
                // 历史记录已解除或已过期：沿用原 banId 就地更新，重新发号会命中不到目标行
                // 注意这里会把 operatorAccountId 覆盖为当前操作者，即"谁最后禁言谁留名"
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
            channelEventNotifier.channelChanged(afterCommit, channel.id(), ChannelChangeScope.BANS, recipients);
            return ChannelBanResult.from(ban);
        });
    }

    @Override
    public ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command) {
        Long durationSeconds = null;
        if (command.untilEpochMillis() != null) {
            long nowMillis = timeProvider.nowMillis();
            if (command.untilEpochMillis() <= nowMillis) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "until must be greater than current time");
            }
            // 向下取整到秒并保底 1 秒，避免极短间隔被换算成 0 而产生立即到期的禁言
            durationSeconds = Math.max(1L, (command.untilEpochMillis() - nowMillis) / 1000L);
        }
        return banChannelMember(new BanChannelMemberCommand(
            command.operatorAccountId(), command.channelId(), command.targetAccountId(),
            command.reason(), durationSeconds));
    }

    @Override
    public void unbanChannelMember(long operatorAccountId, long channelId, long targetAccountId) {
        transactionRunner.runInTransaction(afterCommit -> {
            // 查询基础信息
            Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
            ChannelMember operator = channelMemberRepository.findByRelation(channelId, operatorAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
            // 禁言解除同样属于治理动作，普通成员不可执行
            if (operator.role() != ChannelMemberRole.OWNER
                && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.of(ChannelProblem.BAN_FORBIDDEN);
            }
            ChannelBan existingBan = channelBanRepository.findByRelation(
                    channel.id(), targetAccountId)
                .orElseThrow(() -> ProblemException.of(ChannelProblem.BAN_NOT_FOUND));
            // 只有仍然生效的记录才可解除：已解除或已过期的记录重复解除按参数错误拒绝
            if (existingBan.revokedAt() != null
                || (existingBan.expiresAt() != null && !existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.of(ChannelProblem.INVALID_INPUT, "channel ban is not active");
            }
            // 领域模型不可变：沿用原 banId、原原因与原到期时间，只补充解除时间
            // 注意 operatorAccountId 会被覆盖为解禁人，因此该字段表达的是"最后操作者"而非"最初禁言人"
            ChannelBan revokedBan = new ChannelBan(
                existingBan.banId(),
                existingBan.channelId(), existingBan.bannedAccountId(), operator.accountId(),
                existingBan.reason(), existingBan.expiresAt(), existingBan.createdAt(), timeProvider.nowInstant());
            channelBanRepository.update(revokedBan);
            // 通知频道成员刷新禁言台账，并单独通知被解禁者刷新自身状态
            channelEventNotifier.channelChanged(
                afterCommit,
                channel.id(),
                ChannelChangeScope.BANS,
                channelMemberRepository.listByChannelId(channel.id()).stream().map(ChannelMember::accountId).toList());
        });
    }

}
