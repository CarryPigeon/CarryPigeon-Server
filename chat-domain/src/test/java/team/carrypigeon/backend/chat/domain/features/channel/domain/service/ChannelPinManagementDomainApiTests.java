package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RemoveChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.SetChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelPinReferencesQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `ChannelPinManagementDomainApi` 契约测试。
 * 职责：验证 channel 拥有的置顶权限、上限、替换、取消和成员列表业务契约。
 * 边界：不读取消息模型，也不验证 realtime 事件发布。
 */
@Tag("contract")
class ChannelPinManagementDomainApiTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

    /**
     * 验证频道所有者可以创建置顶，并由 channel 规范化备注后写入仓储。
     */
    @Test
    @DisplayName("set pin owner persists normalized pin")
    void setPin_owner_persistsNormalizedPin() {
        Fixture fixture = fixture(ChannelMemberRole.OWNER);
        when(fixture.pinRepository.findByChannelIdAndMessageId(1L, 5001L)).thenReturn(Optional.empty());
        when(fixture.pinRepository.countByChannelId(1L)).thenReturn(0L);

        ChannelPinReference result = fixture.api.setPin(command(9001L, " notice "));

        ArgumentCaptor<ChannelPin> captor = ArgumentCaptor.forClass(ChannelPin.class);
        verify(fixture.pinRepository).save(captor.capture());
        assertEquals(new ChannelPin(9001L, 1L, 5001L, 1001L, "notice", NOW), captor.getValue());
        assertEquals(9001L, result.pinId());
        assertEquals("notice", result.note());
    }

    /**
     * 验证重复置顶会先删除旧记录再写入新记录，兼容真实数据库联合主键约束。
     */
    @Test
    @DisplayName("set pin existing pin replaces record")
    void setPin_existingPin_replacesRecord() {
        Fixture fixture = fixture(ChannelMemberRole.OWNER);
        ChannelPin existing = new ChannelPin(8001L, 1L, 5001L, 1001L, "old", NOW.minusSeconds(60));
        when(fixture.pinRepository.findByChannelIdAndMessageId(1L, 5001L)).thenReturn(Optional.of(existing));

        fixture.api.setPin(command(9001L, "new"));

        InOrder order = inOrder(fixture.pinRepository);
        order.verify(fixture.pinRepository).delete(1L, 5001L);
        order.verify(fixture.pinRepository).save(new ChannelPin(9001L, 1L, 5001L, 1001L, "new", NOW));
        verify(fixture.pinRepository, never()).countByChannelId(1L);
    }

    /**
     * 验证新增置顶超过频道上限时返回稳定校验问题且不写仓储。
     */
    @Test
    @DisplayName("set pin full channel throws pin limit problem")
    void setPin_fullChannel_throwsPinLimitProblem() {
        Fixture fixture = fixture(ChannelMemberRole.OWNER);
        when(fixture.pinRepository.findByChannelIdAndMessageId(1L, 5001L)).thenReturn(Optional.empty());
        when(fixture.pinRepository.countByChannelId(1L)).thenReturn(50L);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.api.setPin(command(9001L, "notice"))
        );

        assertEquals("pin_limit_reached", exception.reason());
        verify(fixture.pinRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证普通成员不能创建或替换频道置顶。
     */
    @Test
    @DisplayName("set pin member throws forbidden")
    void setPin_member_throwsForbidden() {
        Fixture fixture = fixture(ChannelMemberRole.MEMBER);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.api.setPin(command(9001L, "notice"))
        );

        assertEquals("channel_pin_forbidden", exception.reason());
    }

    /**
     * 验证取消置顶会返回删除前快照并删除目标记录。
     */
    @Test
    @DisplayName("remove pin existing pin returns snapshot and deletes")
    void removePin_existingPin_returnsSnapshotAndDeletes() {
        Fixture fixture = fixture(ChannelMemberRole.OWNER);
        ChannelPin existing = new ChannelPin(8001L, 1L, 5001L, 1001L, "old", NOW);
        when(fixture.pinRepository.findByChannelIdAndMessageId(1L, 5001L)).thenReturn(Optional.of(existing));

        ChannelPinReference result = fixture.api.removePin(new RemoveChannelPinCommand(1L, 5001L, 1001L));

        assertEquals(8001L, result.pinId());
        verify(fixture.pinRepository).delete(1L, 5001L);
    }

    /**
     * 验证频道成员可以按游标读取稳定置顶引用列表。
     */
    @Test
    @DisplayName("list pins member returns references")
    void listPins_member_returnsReferences() {
        Fixture fixture = fixture(ChannelMemberRole.MEMBER);
        ChannelPin pin = new ChannelPin(8001L, 1L, 5001L, 1001L, "notice", NOW);
        when(fixture.pinRepository.findByChannelIdBefore(1L, 5002L, 20)).thenReturn(List.of(pin));

        List<ChannelPinReference> result = fixture.api.listPins(
                new ListChannelPinReferencesQuery(1001L, 1L, 5002L, 20)
        );

        assertEquals(List.of(5001L), result.stream().map(ChannelPinReference::messageId).toList());
    }

    private SetChannelPinCommand command(long pinId, String note) {
        return new SetChannelPinCommand(pinId, 1L, 5001L, 1001L, note, NOW);
    }

    private Fixture fixture(ChannelMemberRole role) {
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
        ChannelPinRepository pinRepository = mock(ChannelPinRepository.class);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(
                new Channel(1L, 11L, "project", "", "", "1001", "private", false, NOW, NOW)
        ));
        when(memberRepository.findByChannelIdAndAccountId(1L, 1001L)).thenReturn(Optional.of(
                new ChannelMember(1L, 1001L, role, NOW, null)
        ));
        ChannelPinManagementDomainApi api = new ChannelPinManagementDomainApi(
                new ChannelMembershipService(channelRepository, memberRepository),
                pinRepository,
                new ChannelGovernancePolicy()
        );
        return new Fixture(api, pinRepository);
    }

    private record Fixture(ChannelPinManagementDomainApi api, ChannelPinRepository pinRepository) {
    }
}
