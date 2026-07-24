package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InitializeChannelMembershipsCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 新账号频道成员关系初始化 API 实现。
 * 职责：在 channel feature 内建立默认频道与 system 频道成员关系。
 * 边界：不读取 auth 或 user 内部模型。
 */
@Service
public class ChannelAccountProvisioningDomainApi implements ChannelAccountProvisioningApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;

    public ChannelAccountProvisioningDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
    }

    @Override
    public void initializeMemberships(InitializeChannelMembershipsCommand command) {
        Channel defaultChannel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.fail("default_channel_missing", "default channel does not exist"));
        channelMemberRepository.save(new ChannelMember(
                defaultChannel.id(),
                command.accountId(),
                ChannelMemberRole.MEMBER,
                command.joinedAt(),
                null
        ));
        Channel systemChannel = channelRepository.findSystemChannel()
                .orElseThrow(() -> ProblemException.fail("system_channel_missing", "system channel does not exist"));
        if (!channelMemberRepository.exists(systemChannel.id(), command.accountId())) {
            channelMemberRepository.save(new ChannelMember(
                    systemChannel.id(),
                    command.accountId(),
                    ChannelMemberRole.MEMBER,
                    command.joinedAt(),
                    null
            ));
        }
    }
}
