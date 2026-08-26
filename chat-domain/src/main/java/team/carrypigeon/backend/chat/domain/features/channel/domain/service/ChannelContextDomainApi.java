package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;

/**
 * 频道上下文领域 API 实现。
 * 职责：提供频道存在性、成员关系和接收账号快照查询。
 */
@Service
public class ChannelContextDomainApi implements ChannelContextApi {

    private final ChannelMembershipService membershipService;
    private final ChannelMemberRepository channelMemberRepository;

    public ChannelContextDomainApi(
            ChannelMembershipService membershipService,
            ChannelMemberRepository channelMemberRepository
    ) {
        this.membershipService = membershipService;
        this.channelMemberRepository = channelMemberRepository;
    }

    @Override
    public ChannelMessagingContext requireChannel(long channelId) {
        return membershipService.toContext(membershipService.requireChannel(channelId));
    }

    @Override
    public ChannelMessagingContext requireMemberChannel(long channelId, long accountId) {
        Channel channel = membershipService.requireChannel(channelId);
        membershipService.requireMembership(channel.id(), accountId);
        return membershipService.toContext(channel);
    }

    @Override
    public boolean isMember(long channelId, long accountId) {
        return channelMemberRepository.exists(channelId, accountId);
    }

    @Override
    public List<Long> recipientAccountIds(long channelId) {
        return channelMemberRepository.findAccountIdsByChannelId(channelId);
    }
}
