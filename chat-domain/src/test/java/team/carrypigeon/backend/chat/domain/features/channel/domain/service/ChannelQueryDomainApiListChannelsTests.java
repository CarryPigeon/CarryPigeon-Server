package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 频道列表查询契约测试。
 * 职责：验证频道列表使用批量频道与 owner 快照组装结果，并保持稳定顺序与去重语义。
 * 边界：不访问真实数据库，不验证 HTTP 响应包装。
 */
@Tag("contract")
class ChannelQueryDomainApiListChannelsTests {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    /** 验证成员频道通过两个批量端口读取，且不会逐频道查询详情或扫描成员。 */
    @Test
    @DisplayName("list channels member channels uses batch queries")
    void listChannels_memberChannels_usesBatchQueries() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        Channel defaultChannel = channel(1L, "public", true);
        Channel projectChannel = channel(9L, "private", false);
        Channel teamChannel = channel(10L, "private", false);
        when(channelRepository.findDefaultChannel()).thenReturn(Optional.of(defaultChannel));
        when(channelRepository.findSystemChannel()).thenReturn(Optional.empty());
        when(memberRepository.findChannelIdsByAccountId(1001L)).thenReturn(List.of(9L, 10L));
        when(channelRepository.findByIds(List.of(9L, 10L))).thenReturn(Map.of(9L, projectChannel, 10L, teamChannel));
        when(memberRepository.findOwnerAccountIdsByChannelIds(java.util.Set.of(1L, 9L, 10L)))
                .thenReturn(Map.of(1L, 1001L, 9L, 1002L, 10L, 1003L));
        ChannelQueryDomainApi api = new ChannelQueryDomainApi(
                channelRepository,
                memberRepository,
                mock(ChannelBanRepository.class),
                mock(ChannelAuditLogRepository.class),
                mock(UserProfileApi.class),
                new ChannelGovernancePolicy()
        );

        var result = api.listChannels(1001L);

        assertEquals(List.of(1L, 9L, 10L), result.stream().map(item -> item.channelId()).toList());
        assertEquals(List.of("1001", "1002", "1003"), result.stream().map(item -> item.ownerUid()).toList());
        verify(channelRepository).findByIds(List.of(9L, 10L));
        verify(channelRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(memberRepository, never()).findByChannelId(org.mockito.ArgumentMatchers.anyLong());
    }

    private Channel channel(long id, String type, boolean defaultChannel) {
        return new Channel(id, id, "channel-" + id, "", "", "", type, defaultChannel, NOW, NOW);
    }
}
