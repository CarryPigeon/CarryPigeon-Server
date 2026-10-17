package team.carrypigeon.backend.chat.domain.features.channel.domain.service.member;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.InitializeChannelMembershipsCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;

import java.util.List;

/**
 * 新账号频道成员关系初始化 API 实现。
 * 职责：为新建账号建立全部系统频道的成员关系，使账号在登录后即可看到默认频道。
 * 边界：不读取 auth 或 user 内部模型，不创建频道本身；系统频道不存在时本实现不做任何写入，也不抛异常。
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
            // 幂等保护：账号已在该频道存在成员关系时跳过，避免重复注册或多链路调用产生重复行
            if (channelMemberRepository.findByRelation(systemChannel.id(), command.accountId()).isEmpty()) {
                // 系统频道的成员统一以 MEMBER 角色加入，不分配 OWNER 或 ADMIN，因此不参与频道治理
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
