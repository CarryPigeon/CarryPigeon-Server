package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagePolicyApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息治理策略 API 实现。
 * 职责：组合频道成员上下文与治理策略，完成发送和撤回权限判定。
 */
@Service
public class ChannelMessagePolicyDomainApi implements ChannelMessagePolicyApi {

    private final ChannelMembershipService membershipService;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;

    public ChannelMessagePolicyDomainApi(
            ChannelMembershipService membershipService,
            ChannelMemberRepository channelMemberRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        this.membershipService = membershipService;
        this.channelMemberRepository = channelMemberRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
    }

    @Override
    public ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now) {
        Channel channel = membershipService.requireChannel(channelId);
        ChannelMember member = membershipService.requireMembership(channel.id(), accountId);
        channelGovernancePolicy.requireCanSendMessage(channel, member, now);
        return membershipService.toContext(channel);
    }

    @Override
    public void requireRecallPermission(RequireMessageRecallPermissionCommand command) {
        Channel channel = membershipService.requireChannel(command.channelId());
        ChannelMember operator = membershipService.requireMembership(channel.id(), command.operatorAccountId());
        if (command.messageChannelId() != channel.id()) {
            throw ProblemException.notFound("message does not exist");
        }
        ChannelMember sender = channelMemberRepository
                .findByChannelIdAndAccountId(channel.id(), command.senderAccountId())
                .orElse(null);
        channelGovernancePolicy.requireCanRecallMessage(channel, operator, command.senderAccountId(), sender);
    }
}
