package team.carrypigeon.backend.chat.domain.features.channel.domain.service.context;

import java.util.List;
import java.time.Instant;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.input.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道上下文领域 API 实现。
 * 职责：提供频道存在性、成员关系和接收账号快照查询。
 */
@Service
public class ChannelContextDomainApi implements ChannelContextApi {

    private final ChannelMembershipService membershipService;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    public ChannelContextDomainApi(
            ChannelMembershipService membershipService,
            ChannelMemberRepository channelMemberRepository,
            ChannelBanRepository channelBanRepository
    ) {
        this.membershipService = membershipService;
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
    }

    @Override
    public ChannelMessagingContext requireChannel(long channelId) {
        return ChannelMessagingContext.from(membershipService.requireChannel(channelId));
    }

    @Override
    public ChannelMessagingContext requireMemberChannel(long channelId, long accountId) {
        Channel channel = membershipService.requireChannel(channelId);
        membershipService.requireMembership(channel.id(), accountId);
        return ChannelMessagingContext.from(channel);
    }

    @Override
    public boolean isMember(long channelId, long accountId) {
        return channelMemberRepository.exists(channelId, accountId);
    }

    @Override
    public List<Long> recipientAccountIds(long channelId) {
        return channelMemberRepository.findAccountIdsByChannelId(channelId);
    }

    @Override
    public ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now) {
        Channel channel = membershipService.requireChannel(channelId);
        ChannelMember member = membershipService.requireMembership(channel.id(), accountId);
        ChannelBan channelBan = channelBanRepository
                .findByChannelIdAndBannedAccountId(channel.id(), accountId).orElse(null);
        if (channelBan != null
                && channelBan.revokedAt() == null
                && (channelBan.expiresAt() == null || channelBan.expiresAt().isAfter(now))) {
            throw ProblemException.forbidden("user_muted", "channel member is muted");
        }
        return ChannelMessagingContext.from(channel);
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
        if (operator.accountId() != command.senderAccountId()) {
            if (!channel.systemChannel()) {
                throw ProblemException.forbidden(
                        "channel_message_recall_forbidden",
                        "channel message recall is not allowed"
                );
            }
            if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
                throw ProblemException.forbidden(
                        "channel_message_recall_forbidden",
                        "channel message recall is not allowed"
                );
            }
            if (operator.role() == ChannelMemberRole.ADMIN && sender != null
                    && sender.role() != ChannelMemberRole.MEMBER) {
                throw ProblemException.forbidden(
                        "channel_message_recall_forbidden",
                        sender.role() == ChannelMemberRole.OWNER
                                ? "target member with OWNER role cannot be moderated"
                                : "channel message recall is not allowed"
                );
            }
        }
    }
}
