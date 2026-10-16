package team.carrypigeon.backend.chat.domain.features.channel.domain.service.member;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.InitializeChannelMembershipsCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;

import java.util.List;

/**
 * 新账号频道成员关系初始化 API 实现。
 * 职责：在 channel feature 内建立默认频道与 system 频道成员关系。
 * 边界：不读取 auth 或 user 内部模型。
 */
@Service
public class ChannelMemberProvisioningDomainApi implements ChannelMemberProvisioningApi {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final IdGenerator idGenerator;

    public ChannelMemberProvisioningDomainApi(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository, IdGenerator idGenerator
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.idGenerator = idGenerator;
    }

    @Override
    public void initializeMemberships(InitializeChannelMembershipsCommand command) {
        List<Channel> systemChannels = channelRepository.listSystemChannels();
        for (Channel systemChannel : systemChannels) {
            if (channelMemberRepository.findByRelation(systemChannel.id(), command.accountId()).isEmpty()) {
                channelMemberRepository.save(new ChannelMember(
                    idGenerator.nextLongId(),
                    systemChannel.id(),
                    command.accountId(),
                    ChannelMemberRole.MEMBER,
                    command.joinedAt()
                ));
            }
        }
    }
}
