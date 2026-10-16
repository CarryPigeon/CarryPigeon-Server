package team.carrypigeon.backend.chat.domain.features.channel.domain.service.context;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.input.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道上下文领域 API 实现。
 * 职责：提供频道存在性、成员关系和接收账号快照查询。
 */
@Service
public class ChannelContextDomainApi implements ChannelContextApi {

    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelRepository channelRepository;

    public ChannelContextDomainApi(
        ChannelMemberRepository channelMemberRepository,
        ChannelBanRepository channelBanRepository,
        ChannelRepository channelRepository
    ) {
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelRepository = channelRepository;
    }

    @Override
    public ChannelMessagingContext requireChannel(long channelId) {
        return ChannelMessagingContext.from(channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.notFound("channel does not exist")));
    }

    @Override
    public ChannelMessagingContext requireMemberChannel(long channelId, long accountId) {
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        return ChannelMessagingContext.from(channel);
    }

    @Override
    public boolean isMember(long channelId, long accountId) {
        return channelMemberRepository.findByRelation(channelId, accountId).isPresent();
    }

    @Override
    public List<Long> recipientAccountIds(long channelId) {
        return channelMemberRepository.listByChannelId(channelId).stream().map(ChannelMember::accountId).toList();
    }

    @Override
    public ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now) {
        Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        channelMemberRepository.findByRelation(channelId, accountId)
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        ChannelBan channelBan = channelBanRepository
            .findByRelation(channel.id(), accountId).orElse(null);
        if (channelBan != null
            && channelBan.revokedAt() == null
            && (channelBan.expiresAt() == null || channelBan.expiresAt().isAfter(now))) {
            throw ProblemException.forbidden("user_muted", "channel member is muted");
        }
        return ChannelMessagingContext.from(channel);
    }

    @Override
    public void requireRecallPermission(RequireMessageRecallPermissionCommand command) {
        // 拉取基础信息
        Channel channel = channelRepository.findById(command.channelId())
            .orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        ChannelMember operator = channelMemberRepository.findByRelation(command.channelId(), command.operatorAccountId())
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        ChannelMember sender = channelMemberRepository.findByRelation(command.channelId(), command.senderAccountId())
            .orElseThrow(() -> ProblemException.forbidden("not_channel_member", "channel membership is required"));
        // 权限校验
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
            if (operator.role() == ChannelMemberRole.ADMIN && sender.role() != ChannelMemberRole.MEMBER) {
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
