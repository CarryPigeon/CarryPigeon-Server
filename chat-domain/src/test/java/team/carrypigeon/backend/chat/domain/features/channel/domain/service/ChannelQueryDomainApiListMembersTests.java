package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 频道成员列表查询契约测试。
 * 职责：验证成员资料通过一次批量 API 调用组装，并保持成员顺序和缺失资料语义。
 * 边界：不访问数据库和 HTTP 协议层。
 */
@Tag("contract")
class ChannelQueryDomainApiListMembersTests {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    /** 验证多名成员只触发一次公开资料查询，缺失资料仍返回空展示字段。 */
    @Test
    @DisplayName("list channel members multiple members batches profiles")
    void listChannelMembers_multipleMembers_batchesProfiles() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        UserProfileApi userProfileApi = mock(UserProfileApi.class);
        Channel channel = new Channel(9L, 9L, "project", "", "", "", "private", false, NOW, NOW);
        ChannelMember owner = new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, NOW, null);
        ChannelMember member = new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, NOW.plusSeconds(1), null);
        when(channelRepository.findById(9L)).thenReturn(Optional.of(channel));
        when(memberRepository.findByChannelIdAndAccountId(9L, 1001L)).thenReturn(Optional.of(owner));
        when(memberRepository.findByChannelId(9L)).thenReturn(List.of(owner, member));
        when(userProfileApi.getPublicUserProfiles(List.of(1001L, 1002L))).thenReturn(List.of(
                new UserProfileResult(1001L, "owner", "owner.png", "", 0L, 0L, NOW, NOW)
        ));
        ChannelQueryDomainApi api = new ChannelQueryDomainApi(
                channelRepository,
                memberRepository,
                mock(ChannelBanRepository.class),
                mock(ChannelAuditLogRepository.class),
                userProfileApi,
                new ChannelGovernancePolicy()
        );

        var result = api.listChannelMembers(new ListChannelMembersQuery(1001L, 9L));

        assertEquals(List.of(1001L, 1002L), result.stream().map(item -> item.accountId()).toList());
        assertEquals("owner", result.getFirst().nickname());
        assertEquals("", result.getLast().nickname());
        assertEquals("", result.getLast().avatarUrl());
        verify(userProfileApi, times(1)).getPublicUserProfiles(List.of(1001L, 1002L));
    }
}
