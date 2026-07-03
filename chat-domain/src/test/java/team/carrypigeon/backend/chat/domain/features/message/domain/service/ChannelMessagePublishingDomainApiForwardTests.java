package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * `ChannelMessagePublishingDomainApiForward` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class ChannelMessagePublishingDomainApiForwardTests {

    /**
     * 验证 `forwardChannelMessage` 在 `createsNewTargetChannelMessage` 场景下的测试契约。
     */
    @Test
    @DisplayName("forward channel message creates new target channel message")
    void forwardChannelMessage_createsNewTargetChannelMessage() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new Channel(1L, 1L, "source", "", "", "", "private", false, MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME));
        fixture.channelRepository.channels.put(2L, new Channel(2L, 2L, "target", "", "", "", "private", false, MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME));
        fixture.channelMemberRepository.memberships.put(2L, new ArrayList<>(List.of(
                new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, MessageDomainApiTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5001L, new ChannelMessage(5001L, MessageDomainApiTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "hello", "hello", "hello", null, null, "sent", MessageDomainApiTestSupport.BASE_TIME));

        ChannelMessageResult result = fixture.publishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(1001L, 5001L, 2L, "FYI"));

        assertEquals(2L, result.channelId());
        assertEquals(true, result.body().contains("[Forwarded] hello"));
        assertEquals(true, result.body().contains("FYI"));
        assertTrue(result.forwardedFrom().contains("\"mid\":\"5001\""));
        assertTrue(result.forwardedFrom().contains("\"cid\":\"1\""));
        assertTrue(result.forwardedFrom().contains("\"uid\":\"1002\""));
    }
}
