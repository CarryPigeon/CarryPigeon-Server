package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道领域投影 mapper。
 * 职责：集中组装频道和频道成员领域投影，并读取 owner 与成员用户资料快照。
 * 边界：只做投影映射，不校验权限、不修改领域状态。
 */
class ChannelProjectionMapper {

    private final ChannelMemberRepository channelMemberRepository;
    private final UserProfileApi userProfileApi;

    ChannelProjectionMapper(
            ChannelMemberRepository channelMemberRepository,
            UserProfileApi userProfileApi
    ) {
        this.channelMemberRepository = channelMemberRepository;
        this.userProfileApi = userProfileApi;
    }

    ChannelResult toResult(Channel channel) {
        return toResult(channel, findOwnerUid(channel.id()));
    }

    ChannelResult toResult(Channel channel, String ownerUid) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                ownerUid,
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }

    ChannelMemberResult toMemberResult(ChannelMember member) {
        UserProfileResult userProfile = userProfileApi.getPublicUserProfiles(java.util.List.of(member.accountId())).stream()
                .findFirst()
                .orElse(null);
        return toMemberResult(member, userProfile);
    }

    /**
     * 批量组装成员投影，一次读取全部公开用户资料并保持成员输入顺序。
     */
    List<ChannelMemberResult> toMemberResults(List<ChannelMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = members.stream().map(ChannelMember::accountId).toList();
        Map<Long, UserProfileResult> profilesByAccountId = userProfileApi.getPublicUserProfiles(accountIds).stream()
                .collect(Collectors.toMap(UserProfileResult::accountId, Function.identity(), (left, right) -> left));
        return members.stream()
                .map(member -> toMemberResult(member, profilesByAccountId.get(member.accountId())))
                .toList();
    }

    private ChannelMemberResult toMemberResult(ChannelMember member, UserProfileResult userProfile) {
        return new ChannelMemberResult(
                member.accountId(),
                userProfile == null ? "" : userProfile.avatarUrl(),
                member.role().name(),
                member.joinedAt(),
                member.mutedUntil()
        );
    }

    String findOwnerUid(long channelId) {
        Long ownerAccountId = channelMemberRepository
                .findOwnerAccountIdsByChannelIds(List.of(channelId))
                .get(channelId);
        return ownerAccountId == null ? "" : IdUtil.toString(ownerAccountId);
    }
}
