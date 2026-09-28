package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息协作的成员上下文加载器。
 * 职责：统一频道存在性和成员身份加载规则。
 * 边界：仅供 channel feature 内部的公开 API 实现复用，不承载治理决策或写操作。
 */
@Component
public class ChannelMembershipService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;

    public ChannelMembershipService(ChannelRepository channelRepository, ChannelMemberRepository channelMemberRepository) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
    }

    Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
    }

    ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
    }

    ChannelMessagingContext toContext(Channel channel) {
        return new ChannelMessagingContext(channel.id(), channel.type());
    }
}
