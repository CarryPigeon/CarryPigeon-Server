package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.application.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("contract")
class MessageApplicationServicePinsTests {

    @Test
    @DisplayName("pin channel message owner creates pin")
    void pinChannelMessage_owner_createsPin() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(1L, 1L, "project-alpha", "", "", "", "private", false, MessageApplicationServiceTestSupport.BASE_TIME, MessageApplicationServiceTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, MessageApplicationServiceTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5001L, new ChannelMessage(5001L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "hello", "hello", "hello", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME));

        ChannelPinResult result = fixture.service.pinChannelMessage(new PinChannelMessageCommand(1001L, 1L, 5001L, "重要通知"));

        assertEquals(5001L, result.messageId());
        assertEquals(1, fixture.channelPinRepository.pins.size());
        assertEquals(1, fixture.publisher.pinnedMessages.size());
        assertEquals(5001L, fixture.publisher.pinnedMessages.getFirst().messageId());
    }

    @Test
    @DisplayName("unpin channel message removes pin")
    void unpinChannelMessage_removesPin() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(1L, 1L, "project-alpha", "", "", "", "private", false, MessageApplicationServiceTestSupport.BASE_TIME, MessageApplicationServiceTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, MessageApplicationServiceTestSupport.BASE_TIME, null)
        )));
        fixture.channelPinRepository.save(new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin(
                9001L,
                1L,
                5001L,
                1001L,
                "note",
                Instant.parse("2026-04-22T00:00:00Z")
        ));

        fixture.service.unpinChannelMessage(new UnpinChannelMessageCommand(1001L, 1L, 5001L));

        assertEquals(0, fixture.channelPinRepository.pins.size());
        assertEquals(1, fixture.publisher.unpinnedMessages.size());
        assertEquals(5001L, fixture.publisher.unpinnedMessages.getFirst().messageId());
    }

    @Test
    @DisplayName("list channel pins returns ordered pins")
    void listChannelPins_returnsOrderedPins() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
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

        List<ChannelPinResult> result = fixture.service.listChannelPins(new ListChannelPinsQuery(1001L, 1L, null, 20));

        assertEquals(2, result.size());
        assertEquals(5002L, result.getFirst().messageId());
        assertFalse(result.getFirst().pinId() <= 0L);
    }
}
