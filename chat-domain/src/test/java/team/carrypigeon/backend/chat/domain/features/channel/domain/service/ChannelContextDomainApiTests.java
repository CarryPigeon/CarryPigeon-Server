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
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * `ChannelContextDomainApi` 契约测试。
 * 职责：验证跨 feature 频道上下文只暴露最小投影，并保持成员查询的稳定失败语义。
 * 边界：不验证消息治理、置顶、审计或持久化实现。
 */
@Tag("contract")
class ChannelContextDomainApiTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

    /**
     * 验证成员频道查询返回跨 feature 所需的最小上下文。
     */
    @Test
    @DisplayName("require member channel member returns messaging context")
    void requireMemberChannel_member_returnsMessagingContext() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel()));
        when(memberRepository.findByChannelIdAndAccountId(1L, 1001L)).thenReturn(Optional.of(member(1001L)));
        ChannelContextDomainApi api = api(channelRepository, memberRepository);

        ChannelMessagingContext result = api.requireMemberChannel(1L, 1001L);

        assertEquals(1L, result.id());
        assertEquals(11L, result.conversationId());
        assertEquals("private", result.type());
    }

    /**
     * 验证频道不存在时在成员查询前返回 not found 领域问题。
     */
    @Test
    @DisplayName("require channel missing channel throws not found problem")
    void requireChannel_missingChannel_throwsNotFoundProblem() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        when(channelRepository.findById(1L)).thenReturn(Optional.empty());
        ChannelContextDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(ProblemException.class, () -> api.requireChannel(1L));

        assertEquals("not_found", exception.reason());
    }

    /**
     * 验证非成员保持稳定的权限失败 reason。
     */
    @Test
    @DisplayName("require member channel missing membership throws forbidden problem")
    void requireMemberChannel_missingMembership_throwsForbiddenProblem() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel()));
        when(memberRepository.findByChannelIdAndAccountId(1L, 1002L)).thenReturn(Optional.empty());
        ChannelContextDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> api.requireMemberChannel(1L, 1002L)
        );

        assertEquals("not_channel_member", exception.reason());
    }

    /**
     * 验证成员判断和事件接收账号查询直接委托给成员仓储。
     */
    @Test
    @DisplayName("member queries repository values returns unchanged")
    void memberQueries_repositoryValues_returnsUnchanged() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        when(memberRepository.exists(1L, 1002L)).thenReturn(false);
        when(memberRepository.findAccountIdsByChannelId(1L)).thenReturn(List.of(1001L, 1003L));
        ChannelContextDomainApi api = api(channelRepository, memberRepository);

        assertFalse(api.isMember(1L, 1002L));
        assertEquals(List.of(1001L, 1003L), api.recipientAccountIds(1L));
    }

    private ChannelContextDomainApi api(
            ChannelRepository channelRepository,
            ChannelMemberRepository memberRepository
    ) {
        return new ChannelContextDomainApi(
                new ChannelMembershipService(channelRepository, memberRepository),
                memberRepository
        );
    }

    private Channel channel() {
        return new Channel(1L, 11L, "project", "", "", "1001", "private", false, NOW, NOW);
    }

    private ChannelMember member(long accountId) {
        return new ChannelMember(1L, accountId, ChannelMemberRole.MEMBER, NOW, null);
    }
}
