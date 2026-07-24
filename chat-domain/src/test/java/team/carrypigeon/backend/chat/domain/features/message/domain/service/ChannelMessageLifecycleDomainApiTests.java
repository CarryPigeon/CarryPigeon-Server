package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息生命周期契约测试。
 * 职责：验证 sent 到 recalled 的唯一状态转换、脱敏和治理权限。
 * 边界：不验证 HTTP、Netty 和真实数据库访问。
 */
@Tag("contract")
class ChannelMessageLifecycleDomainApiTests {

    /**
     * 验证撤回会保留消息身份并清空 canonical 内容与提醒目标。
     */
    @Test
    @DisplayName("recall sent message redacts canonical content")
    void recallChannelMessage_sentMessage_redactsCanonicalContent() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, message(5001L, 1001L, MessageStatus.SENT));

        ChannelMessageResult result = fixture.lifecycleApi.recallChannelMessage(
                new RecallChannelMessageCommand(1001L, 1L, 5001L)
        );

        assertEquals(5001L, result.messageId());
        assertEquals(MessageStatus.RECALLED, result.status());
        assertTrue(result.data().isEmpty());
        assertTrue(result.mentions().isEmpty());
        assertEquals("消息已撤回", result.preview());
        assertEquals("MESSAGE_RECALLED", fixture.channelAuditLogRepository.logs.getFirst().actionType());
        assertEquals(1, fixture.messageRepository.updatedMessages.size());
        assertEquals(0, fixture.mentionRepository.mentions.size());
    }

    /**
     * 验证重复撤回已撤回消息不会重复写入或广播。
     */
    @Test
    @DisplayName("recall recalled message is idempotent")
    void recallChannelMessage_recalledMessage_isIdempotent() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, message(5001L, 1001L, MessageStatus.RECALLED));

        ChannelMessageResult result = fixture.lifecycleApi.recallChannelMessage(
                new RecallChannelMessageCommand(1001L, 1L, 5001L)
        );

        assertEquals(MessageStatus.RECALLED, result.status());
        assertEquals(0, fixture.messageRepository.updatedMessages.size());
        assertEquals(0, fixture.channelAuditLogRepository.logs.size());
    }

    /**
     * 验证 private channel 管理员可撤回成员消息但不能撤回 owner 消息。
     */
    @Test
    @DisplayName("recall private channel message follows moderation hierarchy")
    void recallChannelMessage_privateChannel_followsModerationHierarchy() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L, 1L, "project-alpha", "", "", "", "private", false,
                MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME
        ));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.ADMIN, MessageDomainApiTestSupport.BASE_TIME, null),
                new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, MessageDomainApiTestSupport.BASE_TIME, null),
                new ChannelMember(1L, 1003L, ChannelMemberRole.OWNER, MessageDomainApiTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5002L, message(5002L, 1002L, MessageStatus.SENT));
        fixture.messageRepository.messagesById.put(5003L, message(5003L, 1003L, MessageStatus.SENT));

        ChannelMessageResult recalled = fixture.lifecycleApi.recallChannelMessage(
                new RecallChannelMessageCommand(1001L, 1L, 5002L)
        );
        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5003L))
        );

        assertEquals(MessageStatus.RECALLED, recalled.status());
        assertEquals("target member with OWNER role cannot be moderated", exception.getMessage());
    }

    private ChannelMessage message(long messageId, long senderId, MessageStatus status) {
        return new ChannelMessage(
                messageId,
                senderId,
                1L,
                "Core:Text",
                "1.0.0",
                status == MessageStatus.RECALLED ? Map.of() : Map.of("text", "hello"),
                MessageDomainApiTestSupport.BASE_TIME,
                status == MessageStatus.RECALLED ? List.of() : List.of(1002L),
                status == MessageStatus.RECALLED ? "消息已撤回" : "hello",
                status
        );
    }
}
