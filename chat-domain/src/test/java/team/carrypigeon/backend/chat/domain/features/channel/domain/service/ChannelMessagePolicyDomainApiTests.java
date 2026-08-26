package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * `ChannelMessagePolicyDomainApi` 契约测试。
 * 职责：验证消息发送和撤回权限通过独立治理边界执行。
 * 边界：不验证消息状态转换、置顶存储或审计写入。
 */
@Tag("contract")
class ChannelMessagePolicyDomainApiTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

    /**
     * 验证未禁言成员通过发送校验后获得最小频道上下文。
     */
    @Test
    @DisplayName("require sendable channel active member returns messaging context")
    void requireSendableChannel_activeMember_returnsMessagingContext() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        stubChannelAndMember(channelRepository, memberRepository, 1001L, ChannelMemberRole.MEMBER, null);
        ChannelMessagePolicyDomainApi api = api(channelRepository, memberRepository);

        ChannelMessagingContext result = api.requireSendableChannel(1L, 1001L, NOW);

        assertEquals(1L, result.id());
        assertEquals(11L, result.conversationId());
    }

    /**
     * 验证私有频道内仍处于禁言期的成员不能发送消息。
     */
    @Test
    @DisplayName("require sendable channel muted member throws user muted")
    void requireSendableChannel_mutedMember_throwsUserMuted() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        stubChannelAndMember(channelRepository, memberRepository, 1001L, ChannelMemberRole.MEMBER, NOW.plusSeconds(60));
        ChannelMessagePolicyDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> api.requireSendableChannel(1L, 1001L, NOW)
        );

        assertEquals("user_muted", exception.reason());
    }

    /**
     * 验证消息不属于目标频道时以 not found 隐藏消息存在性。
     */
    @Test
    @DisplayName("require recall permission different message channel throws not found")
    void requireRecallPermission_differentMessageChannel_throwsNotFound() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        stubChannelAndMember(channelRepository, memberRepository, 1001L, ChannelMemberRole.OWNER, null);
        ChannelMessagePolicyDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> api.requireRecallPermission(new RequireMessageRecallPermissionCommand(1L, 1001L, 2L, 1002L))
        );

        assertEquals("not_found", exception.reason());
    }

    /**
     * 验证普通成员不能撤回其他成员的消息。
     */
    @Test
    @DisplayName("require recall permission member recalls other message throws forbidden")
    void requireRecallPermission_memberRecallsOtherMessage_throwsForbidden() {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        stubChannelAndMember(channelRepository, memberRepository, 1001L, ChannelMemberRole.MEMBER, null);
        when(memberRepository.findByChannelIdAndAccountId(1L, 1002L)).thenReturn(Optional.of(
                new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, NOW, null)
        ));
        ChannelMessagePolicyDomainApi api = api(channelRepository, memberRepository);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> api.requireRecallPermission(new RequireMessageRecallPermissionCommand(1L, 1001L, 1L, 1002L))
        );

        assertEquals("channel_message_recall_forbidden", exception.reason());
    }

    private ChannelMessagePolicyDomainApi api(
            ChannelRepository channelRepository,
            ChannelMemberRepository memberRepository
    ) {
        return new ChannelMessagePolicyDomainApi(
                new ChannelMembershipService(channelRepository, memberRepository),
                memberRepository,
                new ChannelGovernancePolicy()
        );
    }

    private void stubChannelAndMember(
            ChannelRepository channelRepository,
            ChannelMemberRepository memberRepository,
            long accountId,
            ChannelMemberRole role,
            Instant mutedUntil
    ) {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(
                new Channel(1L, 11L, "project", "", "", "1001", "private", false, NOW, NOW)
        ));
        when(memberRepository.findByChannelIdAndAccountId(1L, accountId)).thenReturn(Optional.of(
                new ChannelMember(1L, accountId, role, NOW, mutedUntil)
        ));
    }
}
