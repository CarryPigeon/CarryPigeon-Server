package team.carrypigeon.backend.chat.domain.features.channel.domain.service.context;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道上下文与成员查询领域 API 实现。
 * 职责：承接频道成员上下文读取、成员账号快照与治理账号快照查询，并把禁言生效判定收敛在内部完成。
 * 边界：只读取频道、成员与禁言记录，不承载内容治理动作，也不决定调用方如何使用治理账号快照；不抛业务失败以外的异常。
 */
@Service
public class ChannelContextDomainApi implements ChannelContextApi {

    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelRepository channelRepository;
    private final TimeProvider timeProvider;

    public ChannelContextDomainApi(
        ChannelMemberRepository channelMemberRepository,
        ChannelBanRepository channelBanRepository,
        ChannelRepository channelRepository,
        TimeProvider timeProvider
    ) {
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelRepository = channelRepository;
        this.timeProvider = timeProvider;
    }

    @Override
    public ChannelContext requireMemberContext(long channelId, long accountId) {
        // 频道必须存在，避免把"频道不存在"与"无成员关系"混为一谈
        channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.CHANNEL_NOT_FOUND));
        // 账号必须是该频道成员，非成员一律拒绝；角色与禁言状态随上下文一并返回，调用方无需二次查询
        ChannelMember member = channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.of(ChannelProblem.MEMBERSHIP_REQUIRED));
        return new ChannelContext(member.channelId(), member.role(), activeMutedUntil(channelId, accountId));
    }

    @Override
    public List<Long> memberAccountIds(long channelId) {
        // 只投影账号列，不装配完整成员模型，供广播与 mention 校验使用
        return channelMemberRepository.listByChannelId(channelId).stream()
            .map(ChannelMember::accountId)
            .toList();
    }

    @Override
    public List<Long> moderatorAccounts(long channelId) {
        // OWNER 与 ADMIN 同属治理者集合：调用方据此判定"某成员的内容能否被治理"
        return channelMemberRepository.listByChannelId(channelId).stream()
            .filter(member -> member.role() == ChannelMemberRole.OWNER
                || member.role() == ChannelMemberRole.ADMIN)
            .map(ChannelMember::accountId)
            .toList();
    }

    /**
     * 读取账号在当前时刻仍然生效的禁言到期时刻。
     * 约束：已解除、已过期与不存在记录三种情况统一返回 {@code null}，表示当前未被禁言；
     * 到期时间为空的记录视为不自动到期，因此也不参与"仍在生效"的判定。
     *
     * @param channelId 目标频道 ID
     * @param accountId 待判定账号 ID
     * @return 仍然生效的禁言到期时刻；未处于禁言期时为 {@code null}
     */
    private Instant activeMutedUntil(long channelId, long accountId) {
        // 统一以时间源当前时刻为基准，避免调用方各自传入不同时间造成判定不一致
        Instant now = timeProvider.nowInstant();
        return channelBanRepository.findByRelation(channelId, accountId)
            // 先剔除已解除的记录
            .filter(ban -> ban.revokedAt() == null)
            .map(ChannelBan::expiresAt)
            // 再剔除未设置到期时间与已过期的记录
            .filter(expiresAt -> expiresAt.isAfter(now))
            .orElse(null);
    }
}
