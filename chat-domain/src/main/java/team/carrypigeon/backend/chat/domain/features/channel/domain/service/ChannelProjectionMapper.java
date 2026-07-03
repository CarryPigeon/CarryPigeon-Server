package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 频道领域投影 mapper。
 * 职责：集中组装频道和频道成员领域投影，并读取 owner 与成员用户资料快照。
 * 边界：只做投影映射，不校验权限、不修改领域状态。
 */
class ChannelProjectionMapper {

    private final ChannelMemberRepository channelMemberRepository;
    private final UserProfileRepository userProfileRepository;

    ChannelProjectionMapper(
            ChannelMemberRepository channelMemberRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.channelMemberRepository = channelMemberRepository;
        this.userProfileRepository = userProfileRepository;
    }

    ChannelResult toResult(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                findOwnerUid(channel.id()),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }

    ChannelMemberResult toMemberResult(ChannelMember member) {
        UserProfile userProfile = userProfileRepository.findByAccountId(member.accountId()).orElse(null);
        return new ChannelMemberResult(
                member.accountId(),
                userProfile == null ? "" : userProfile.nickname(),
                userProfile == null ? "" : userProfile.avatarUrl(),
                member.role().name(),
                member.joinedAt(),
                member.mutedUntil()
        );
    }

    String findOwnerUid(long channelId) {
        return channelMemberRepository.findByChannelId(channelId).stream()
                .filter(member -> member.role() == ChannelMemberRole.OWNER)
                .map(member -> Ids.toString(member.accountId()))
                .findFirst()
                .orElse("");
    }
}
