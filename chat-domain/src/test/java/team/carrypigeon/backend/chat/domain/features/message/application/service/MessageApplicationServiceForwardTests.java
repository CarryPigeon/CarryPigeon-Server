package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.application.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("contract")
class MessageApplicationServiceForwardTests {

    @Test
    @DisplayName("forward channel message creates new target channel message")
    void forwardChannelMessage_createsNewTargetChannelMessage() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new Channel(1L, 1L, "source", "", "", "", "private", false, MessageApplicationServiceTestSupport.BASE_TIME, MessageApplicationServiceTestSupport.BASE_TIME));
        fixture.channelRepository.channels.put(2L, new Channel(2L, 2L, "target", "", "", "", "private", false, MessageApplicationServiceTestSupport.BASE_TIME, MessageApplicationServiceTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(2L, new ArrayList<>(List.of(
                new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, MessageApplicationServiceTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5001L, new ChannelMessage(5001L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "hello", "hello", "hello", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME));

        ChannelMessageResult result = fixture.moderationService.forwardChannelMessage(new ForwardChannelMessageCommand(1001L, 5001L, 2L, "FYI"));

        assertEquals(2L, result.channelId());
        assertEquals(true, result.body().contains("[Forwarded] hello"));
        assertEquals(true, result.body().contains("FYI"));
        assertTrue(result.forwardedFrom().contains("\"mid\":\"5001\""));
        assertTrue(result.forwardedFrom().contains("\"cid\":\"1\""));
        assertTrue(result.forwardedFrom().contains("\"uid\":\"1002\""));
    }
}
