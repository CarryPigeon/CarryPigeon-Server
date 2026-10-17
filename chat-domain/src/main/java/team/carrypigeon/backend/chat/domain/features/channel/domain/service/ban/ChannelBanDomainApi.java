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

    /**
     * 查询频道禁言列表。
     * 输入：查询对象携带请求者账号与目标频道。
     * 输出：该频道全部禁言记录的列表项投影，包含已解除与已过期的历史记录。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * @param query 频道禁言列表查询条件
     * @return 禁言列表项投影列表
     */
    @Override
    public List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query) {
        Channel channel = channelRepository.findById(query.channelId())
            .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        ChannelMember operator = channelMemberRepository.findByRelation(query.channelId(), query.accountId())
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        // 禁言台账属于治理数据，普通成员不可读取
        if (operator.role() != ChannelMemberRole.OWNER
            && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(
                "channel_ban_forbidden",
                "channel action requires owner or admin role"
            );
        }
        // 这里不过滤 revokedAt 与 expiresAt，由调用方按需判定记录是否仍然生效
        return channelBanRepository.list(channel.id()).stream()
            .map(ChannelBanListItemResult::from)
            .toList();
    }

    /**
     * 对频道成员执行禁言。
     * 输入：命令携带操作者、目标频道、目标成员、禁言原因与可选持续秒数。
     * 输出：禁言记录投影。
     * 副作用：新建或覆盖该成员的禁言记录，并在事务提交后通知频道成员与目标账号。
     * 约束：调用方必须具备 OWNER 或 ADMIN 角色；目标角色必须是 MEMBER；已生效的禁言不允许重复设置。
     *
     * @param command 频道成员禁言命令
     * @return 禁言结果投影
     */
    @Override
    public ChannelBanResult banChannelMember(BanChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            ChannelMember target = channelMemberRepository.findByRelation(command.channelId(), command.targetAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            // 禁言属于治理动作，普通成员不可执行
            if (operator.role() != ChannelMemberRole.OWNER
                && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                    "channel_ban_forbidden",
                    "channel action requires owner or admin role"
                );
            }
            // 只允许禁言 MEMBER，避免管理员之间互相禁言
            if (target.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.validationFailed("target member must have MEMBER role");
            }
            ChannelBan existingBan = channelBanRepository.findByRelation(
                channel.id(), target.accountId()).orElse(null);
            // 已有记录未解除且未到期时属于重复禁言，按参数错误拒绝
            if (existingBan != null
                && existingBan.revokedAt() == null
                && (existingBan.expiresAt() == null || existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is already active");
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
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new ChannelChangedEvent(channel.id(), "bans", recipients));
            // 被禁言者自身也要刷新状态，它仍在成员快照中但需要单独的偏好与状态更新
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(target.accountId()));
            return ChannelBanResult.from(ban);
        });
    }

    /**
     * 按绝对截止时间对频道成员执行禁言。
     * 输入：命令携带禁言截止毫秒时间戳，为空表示不设置自动到期。
     * 输出：禁言记录投影。
     * 约束：截止时间必须晚于当前时间；协议层的绝对时间在此换算为内部持续秒数后复用禁言用例。
     *
     * @param command 按绝对过期时间的禁言命令
     * @return 禁言结果投影
     */
    @Override
    public ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command) {
        Long durationSeconds = null;
        if (command.untilEpochMillis() != null) {
            long nowMillis = timeProvider.nowMillis();
            if (command.untilEpochMillis() <= nowMillis) {
                throw ProblemException.validationFailed("until must be greater than current time");
            }
            // 向下取整到秒并保底 1 秒，避免极短间隔被换算成 0 而产生立即到期的禁言
            durationSeconds = Math.max(1L, (command.untilEpochMillis() - nowMillis) / 1000L);
        }
        return banChannelMember(new BanChannelMemberCommand(
            command.operatorAccountId(), command.channelId(), command.targetAccountId(),
            command.reason(), durationSeconds));
    }

    /**
     * 解除频道成员的禁言。
     * 输入：命令携带操作者、目标频道与目标成员账号。
     * 输出：解除后的禁言记录投影。
     * 副作用：把禁言记录标记为已解除，并在事务提交后通知频道成员与目标账号。
     * 约束：调用方必须具备 OWNER 或 ADMIN 角色；目标必须存在仍然生效的禁言记录。
     *
     * @param command 解除频道成员禁言命令
     * @return 解除后的禁言结果投影
     */
    @Override
    public ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 查询基础信息
            Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
            ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
            // 禁言解除同样属于治理动作，普通成员不可执行
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
            // 只有仍然生效的记录才可解除：已解除或已过期的记录重复解除按参数错误拒绝
            if (existingBan.revokedAt() != null
                || (existingBan.expiresAt() != null && !existingBan.expiresAt().isAfter(timeProvider.nowInstant()))) {
                throw ProblemException.validationFailed("channel ban is not active");
            }
            // 领域模型不可变：沿用原 banId、原原因与原到期时间，只补充解除时间
            // 注意 operatorAccountId 会被覆盖为解禁人，因此该字段表达的是"最后操作者"而非"最初禁言人"
            ChannelBan revokedBan = new ChannelBan(
                existingBan.banId(),
                existingBan.channelId(), existingBan.bannedAccountId(), operator.accountId(),
                existingBan.reason(), existingBan.expiresAt(), existingBan.createdAt(), timeProvider.nowInstant());
            channelBanRepository.update(revokedBan);
            // 通知频道成员刷新禁言台账，并单独通知被解禁者刷新自身状态
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ChannelChangedEvent(
                    channel.id(), "bans", channelMemberRepository.listByChannelId(channel.id()).stream().map(ChannelMember::accountId).toList()));
            afterCommitEventPublisher.publishAfterCommit(
                afterCommit::execute, new AccountChannelsChangedEvent(command.targetAccountId()));
            return ChannelBanResult.from(revokedBan);
        });
    }

}
