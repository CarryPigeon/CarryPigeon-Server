package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageApplicationService 发送契约测试。
 * 职责：验证文本消息发送入口的应用层编排契约与权限失败语义。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证发送相关语义。
 */
@Tag("contract")
class MessageApplicationServiceSendTests {

    /**
     * 验证发送文本消息时会持久化并使用同一个 messageId 进行实时分发。
     */
    @Test
    @DisplayName("send channel text message valid command persists and publishes same message id")
    void sendChannelTextMessage_validCommand_persistsAndPublishesSameMessageId() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelTextMessage(
                new SendChannelTextMessageCommand(1001L, 1L, "hello world")
        );

        assertEquals(5001L, result.messageId());
        assertEquals(5001L, fixture.messageRepository.savedMessages.getFirst().messageId());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertEquals("carrypigeon-local", result.serverId());
        assertEquals("hello world", result.body());
        assertEquals("hello world", result.previewText());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证通用消息发送入口在 text 草稿场景下保持既有 text 消息语义。
     */
    @Test
    @DisplayName("send channel message text draft preserves text semantics")
    void sendChannelMessage_textDraft_preservesTextSemantics() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelMessage(
                new SendChannelMessageCommand(1001L, 1L, new TextChannelMessageDraft("hello plugin world"))
        );

        assertEquals("text", result.messageType());
        assertEquals("hello plugin world", result.body());
        assertEquals("hello plugin world", result.previewText());
        assertEquals(null, result.payload());
        assertEquals(null, result.metadata());
        assertEquals("sent", result.status());
    }

    /**
     * 验证非成员发送消息时会返回权限问题语义。
     */
    @Test
    @DisplayName("send channel text message non member throws forbidden problem")
    void sendChannelTextMessage_nonMember_throwsForbiddenProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证 private channel 成员被禁言时无法继续发送消息。
     */
    @Test
    @DisplayName("send channel text message muted private member throws forbidden problem")
    void sendChannelTextMessage_mutedPrivateMember_throwsForbiddenProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channel = new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L,
                1L,
                "project-alpha",
                "private",
                false,
                MessageApplicationServiceTestSupport.BASE_TIME,
                MessageApplicationServiceTestSupport.BASE_TIME
        );
        fixture.channelMemberRepository.memberships.put(1L, List.of(
                new ChannelMember(
                        1L,
                        1001L,
                        ChannelMemberRole.MEMBER,
                        MessageApplicationServiceTestSupport.BASE_TIME,
                        MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(300)
                )
        ));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel member is muted", exception.getMessage());
    }

    /**
     * 验证发送者撤回自己的消息时会保留 messageId 并清空可搜索内容。
     */
    @Test
    @DisplayName("recall channel message sender owned message redacts content and publishes update")
    void recallChannelMessage_senderOwnedMessage_redactsContentAndPublishesUpdate() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "hello world",
                "hello world",
                null,
                "{\"extra\":true}",
                "sent",
                MessageApplicationServiceTestSupport.BASE_TIME
        ));

        ChannelMessageResult result = fixture.service.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5001L));

        assertEquals(5001L, result.messageId());
        assertEquals("recalled", result.status());
        assertEquals("[消息已撤回]", result.body());
        assertEquals("[消息已撤回]", result.previewText());
        assertEquals(null, result.payload());
        assertEquals(null, result.metadata());
        assertEquals("", fixture.messageRepository.updatedMessages.getFirst().searchableText());
        assertEquals("recalled", fixture.publisher.publishedMessages.getFirst().status());
        assertEquals("MESSAGE_RECALLED", fixture.channelAuditLogRepository.logs.getFirst().actionType());
    }

    /**
     * 验证 private channel 的 ADMIN 可以撤回 MEMBER 消息，但不能撤回 OWNER 消息。
     */
    @Test
    @DisplayName("recall channel message private admin target rules follow governance policy")
    void recallChannelMessage_privateAdminTargetRules_followGovernancePolicy() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channel = new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L,
                1L,
                "project-alpha",
                "private",
                false,
                MessageApplicationServiceTestSupport.BASE_TIME,
                MessageApplicationServiceTestSupport.BASE_TIME
        );
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.ADMIN, MessageApplicationServiceTestSupport.BASE_TIME, null),
                new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1), null),
                new ChannelMember(1L, 1003L, ChannelMemberRole.OWNER, MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2), null)
        )));
        fixture.messageRepository.messagesById.put(5002L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5002L, "carrypigeon-local", 1L, 1L, 1002L, "text", "member", "member", "member", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));
        fixture.messageRepository.messagesById.put(5003L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5003L, "carrypigeon-local", 1L, 1L, 1003L, "text", "owner", "owner", "owner", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));

        ChannelMessageResult recalled = fixture.service.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5002L));

        assertEquals("recalled", recalled.status());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5003L))
        );

        assertEquals("target member with OWNER role cannot be moderated", exception.getMessage());
    }

    /**
     * 验证 private channel 的 ADMIN 可以撤回 former / non-active 成员消息。
     */
    @Test
    @DisplayName("recall channel message private admin former sender succeeds")
    void recallChannelMessage_privateAdminFormerSender_succeeds() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channel = new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L,
                1L,
                "project-alpha",
                "private",
                false,
                MessageApplicationServiceTestSupport.BASE_TIME,
                MessageApplicationServiceTestSupport.BASE_TIME
        );
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.ADMIN, MessageApplicationServiceTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5004L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5004L, "carrypigeon-local", 1L, 1L, 1999L, "text", "former member", "former member", "former member", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));

        ChannelMessageResult recalled = fixture.service.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5004L));

        assertEquals("recalled", recalled.status());
    }
}
