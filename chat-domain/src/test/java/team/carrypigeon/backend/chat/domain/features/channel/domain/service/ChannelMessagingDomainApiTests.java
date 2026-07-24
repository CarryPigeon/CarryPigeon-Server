package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ChannelMessagingDomainApi 契约测试。
 * 职责：验证 message 等 feature 通过稳定 API 获取频道上下文和成员权限失败语义。
 * 边界：不验证消息聚合、Controller 或数据库适配器。
 */
@Tag("contract")
class ChannelMessagingDomainApiTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

    /**
     * 验证可发送频道只返回跨 feature 所需的最小频道投影。
     */
    @Test
    @DisplayName("require sendable channel member returns messaging context")
    void requireSendableChannel_member_returnsMessagingContext() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        Channel channel = new Channel(1L, 11L, "public", "", "", "", "public", true, NOW, NOW);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(memberRepository.findByChannelIdAndAccountId(1L, 1001L)).thenReturn(Optional.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.MEMBER, NOW, null)
        ));
        ChannelMessagingDomainApi api = api(channelRepository, memberRepository);

        ChannelMessagingContext result = api.requireSendableChannel(1L, 1001L, NOW);

        assertEquals(1L, result.id());
        assertEquals(11L, result.conversationId());
        assertEquals("public", result.type());
    }

    /**
     * 验证非成员调用成员频道入口时保持稳定的权限失败 reason。
     */
    @Test
    @DisplayName("require member channel missing membership throws forbidden problem")
    void requireMemberChannel_missingMembership_throwsForbiddenProblem() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(
                new Channel(1L, 11L, "public", "", "", "", "public", true, NOW, NOW)
        ));
        when(memberRepository.findByChannelIdAndAccountId(1L, 1002L)).thenReturn(Optional.empty());
        ChannelMessagingDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> api.requireMemberChannel(1L, 1002L)
        );

        assertEquals("not_channel_member", exception.reason());
    }

    private ChannelMessagingDomainApi api(
            ChannelRepository channelRepository,
            ChannelMemberRepository memberRepository
    ) {
        return new ChannelMessagingDomainApi(
                channelRepository,
                memberRepository,
                mock(ChannelAuditLogRepository.class),
                mock(ChannelPinRepository.class),
                new ChannelGovernancePolicy()
        );
    }
}
