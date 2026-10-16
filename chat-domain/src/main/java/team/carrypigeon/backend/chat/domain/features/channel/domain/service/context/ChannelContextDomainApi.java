package team.carrypigeon.backend.chat.domain.features.channel.domain.service.context;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 频道上下文与成员查询领域 API 实现。
 * 职责：承接频道成员上下文读取、成员账号快照与治理账号快照查询。
 * 边界：只读取频道、成员与禁言记录，不承载内容治理动作，也不决定调用方如何使用治理账号快照。
 */
@Service
public class ChannelContextDomainApi implements ChannelContextApi {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";

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
        // 频道必须存在，避免把不存在频道与无成员关系混为一谈
        channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        // 账号必须是该频道成员，非成员一律拒绝
        ChannelMember member = channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
        return new ChannelContext(member.channelId(), member.role(), activeMutedUntil(channelId, accountId));
    }

    @Override
    public List<Long> memberAccountIds(long channelId) {
        return channelMemberRepository.listByChannelId(channelId).stream()
            .map(ChannelMember::accountId)
            .toList();
    }

    @Override
    public List<Long> moderatorAccounts(long channelId) {
        return channelMemberRepository.listByChannelId(channelId).stream()
            .filter(member -> member.role() == ChannelMemberRole.OWNER
                || member.role() == ChannelMemberRole.ADMIN)
            .map(ChannelMember::accountId)
            .toList();
    }

    /**
     * 读取账号在当前时刻仍然生效的禁言到期时刻。
     * 约束：已解除、已过期与不存在记录三种情况统一返回 {@code null}，表示当前未被禁言。
     *
     * @param channelId 目标频道 ID
     * @param accountId 待判定账号 ID
     * @return 仍然生效的禁言到期时刻；未处于禁言期时为 {@code null}
     */
    private Instant activeMutedUntil(long channelId, long accountId) {
        Instant now = timeProvider.nowInstant();
        return channelBanRepository.findByRelation(channelId, accountId)
            .filter(ban -> ban.revokedAt() == null)
            .map(ChannelBan::expiresAt)
            .filter(expiresAt -> expiresAt != null && expiresAt.isAfter(now))
            .orElse(null);
    }
}
