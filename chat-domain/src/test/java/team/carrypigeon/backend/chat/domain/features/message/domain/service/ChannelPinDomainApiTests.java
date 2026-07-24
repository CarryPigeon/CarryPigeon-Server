package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
/**
 * `ChannelPinDomainApi` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class ChannelPinDomainApiTests {

    /**
     * 验证 `pinChannelMessage` 在 `owner` 条件下满足 `createsPin` 的测试契约。
     */
    @Test
    @DisplayName("pin channel message owner creates pin")
    void pinChannelMessage_owner_createsPin() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(1L, 1L, "project-alpha", "", "", "", "private", false, MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, MessageDomainApiTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5001L, new ChannelMessage(
                5001L, 1002L, 1L, "Core:Text", "1.0.0", java.util.Map.of("text", "hello"),
                MessageDomainApiTestSupport.BASE_TIME, List.of(), "hello",
                team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus.SENT
        ));

        ChannelPinResult result = fixture.pinApi.pinChannelMessage(new PinChannelMessageCommand(1001L, 1L, 5001L, "重要通知"));

        assertEquals(5001L, result.messageId());
        assertEquals(1, fixture.channelPinRepository.pins.size());
        assertEquals(1, fixture.publisher.pinnedMessages.size());
        assertEquals(5001L, fixture.publisher.pinnedMessages.getFirst().messageId());
    }

    /**
     * 验证 `unpinChannelMessage` 在 `removesPin` 场景下的测试契约。
     */
    @Test
    @DisplayName("unpin channel message removes pin")
    void unpinChannelMessage_removesPin() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(1L, 1L, "project-alpha", "", "", "", "private", false, MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, MessageDomainApiTestSupport.BASE_TIME, null)
        )));
        fixture.channelPinRepository.save(new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin(
                9001L,
                1L,
                5001L,
                1001L,
                "note",
                Instant.parse("2026-04-22T00:00:00Z")
        ));

        fixture.pinApi.unpinChannelMessage(new UnpinChannelMessageCommand(1001L, 1L, 5001L));

        assertEquals(0, fixture.channelPinRepository.pins.size());
        assertEquals(1, fixture.publisher.unpinnedMessages.size());
        assertEquals(5001L, fixture.publisher.unpinnedMessages.getFirst().messageId());
    }

    /**
     * 验证 `listChannelPins` 在 `returnsOrderedPins` 场景下的测试契约。
     */
    @Test
    @DisplayName("list channel pins returns ordered pins")
    void listChannelPins_returnsOrderedPins() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelPinRepository.save(new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin(
                9002L,
                1L,
                5002L,
                1001L,
                "a",
                Instant.parse("2026-04-22T00:00:01Z")
        ));
        fixture.channelPinRepository.save(new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin(
                9001L,
                1L,
                5001L,
                1001L,
                "b",
                Instant.parse("2026-04-22T00:00:00Z")
        ));

        List<ChannelPinResult> result = fixture.pinApi.listChannelPins(new ListChannelPinsQuery(1001L, 1L, null, 20));

        assertEquals(2, result.size());
        assertEquals(5002L, result.getFirst().messageId());
        assertFalse(result.getFirst().pinId() <= 0L);
    }
}
