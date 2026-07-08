package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 消息生命周期契约测试。
 * 职责：验证消息编辑、撤回和治理权限的领域 API 契约。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证生命周期语义。
 */
@Tag("contract")
class ChannelMessageLifecycleDomainApiTests {

    /**
     * 验证发送者撤回自己的消息时会保留 messageId 并清空可搜索内容。
     */
    @Test
    @DisplayName("recall channel message sender owned message redacts content and publishes update")
    void recallChannelMessage_senderOwnedMessage_redactsContentAndPublishesUpdate() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                MessageDomainApiTestSupport.SERVER_ID,
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
                MessageDomainApiTestSupport.BASE_TIME
        ));

        ChannelMessageResult result = fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5001L));

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
     * 验证重复撤回已撤回消息时保持幂等返回且不重复发布事件。
     */
    @Test
    @DisplayName("recall channel message already recalled returns result without publishing update")
    void recallChannelMessage_alreadyRecalled_returnsResultWithoutPublishingUpdate() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                MessageDomainApiTestSupport.SERVER_ID,
                1L,
                1L,
                1001L,
                "text",
                "[消息已撤回]",
                "[消息已撤回]",
                "",
                null,
                null,
                null,
                null,
                "recalled",
                MessageDomainApiTestSupport.BASE_TIME,
                MessageDomainApiTestSupport.BASE_TIME.plusSeconds(1),
                1L
        ));

        ChannelMessageResult result = fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5001L));

        assertEquals(5001L, result.messageId());
        assertEquals("recalled", result.status());
        assertEquals(0, fixture.publisher.publishedMessages.size());
        assertEquals(0, fixture.messageRepository.updatedMessages.size());
        assertEquals(0, fixture.channelAuditLogRepository.logs.size());
    }

    /**
     * 验证发送者编辑自己的消息时会更新正文、版本与 mention。
     */
    @Test
    @DisplayName("edit channel message sender owned message updates content and version")
    void editChannelMessage_senderOwnedMessage_updatesContentAndVersion() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                MessageDomainApiTestSupport.SERVER_ID,
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "hello world",
                "hello world",
                null,
                null,
                null,
                null,
                "sent",
                MessageDomainApiTestSupport.BASE_TIME,
                null,
                1L
        ));

        ChannelMessageResult result = fixture.lifecycleApi.editChannelMessage(new EditChannelMessageCommand(
                1001L,
                5001L,
                "Core:Text",
                "1.0.0",
                "edited content",
                List.of(new EditChannelMessageCommand.MentionTargetCommand("user", 1002L)),
                1L
        ));

        assertEquals("edited content", result.body());
        assertEquals(2L, result.editVersion());
        assertEquals(true, result.mentions().contains("1002"));
        assertEquals("edited content", fixture.messageRepository.updatedMessages.getFirst().body());
        assertEquals(1, fixture.mentionRepository.mentions.size());
        assertEquals(1, fixture.publisher.createdMentions.size());
        assertEquals(1002L, fixture.publisher.createdMentions.getFirst().targetAccountId());
    }

    /**
     * 验证编辑版本不匹配时返回冲突语义。
     */
    @Test
    @DisplayName("edit channel message version mismatch throws conflict")
    void editChannelMessage_versionMismatch_throwsConflict() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                MessageDomainApiTestSupport.SERVER_ID,
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "hello world",
                "hello world",
                null,
                null,
                null,
                null,
                "sent",
                MessageDomainApiTestSupport.BASE_TIME,
                null,
                2L
        ));

        ProblemException exception = assertThrows(ProblemException.class, () -> fixture.lifecycleApi.editChannelMessage(new EditChannelMessageCommand(
                1001L,
                5001L,
                "Core:Text",
                "1.0.0",
                "edited content",
                List.of(),
                1L
        )));

        assertEquals("conflict", exception.reason());
    }

    /**
     * 验证超过编辑窗口时返回禁止语义。
     */
    @Test
    @DisplayName("edit channel message expired window throws forbidden")
    void editChannelMessage_expiredWindow_throwsForbidden() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5001L,
                MessageDomainApiTestSupport.SERVER_ID,
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "hello world",
                "hello world",
                null,
                null,
                null,
                null,
                "sent",
                MessageDomainApiTestSupport.BASE_TIME.minusSeconds(600),
                null,
                1L
        ));

        ProblemException exception = assertThrows(ProblemException.class, () -> fixture.lifecycleApi.editChannelMessage(new EditChannelMessageCommand(
                1001L,
                5001L,
                "Core:Text",
                "1.0.0",
                "edited content",
                List.of(),
                1L
        )));

        assertEquals("message_edit_window_expired", exception.reason());
    }

    /**
     * 验证 private channel 的 ADMIN 可以撤回 MEMBER 消息，但不能撤回 OWNER 消息。
     */
    @Test
    @DisplayName("recall channel message private admin target rules follow governance policy")
    void recallChannelMessage_privateAdminTargetRules_followGovernancePolicy() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L,
                1L,
                "project-alpha",
                "",
                "",
                "",
                "private",
                false,
                MessageDomainApiTestSupport.BASE_TIME,
                MessageDomainApiTestSupport.BASE_TIME
        ));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.ADMIN, MessageDomainApiTestSupport.BASE_TIME, null),
                new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, MessageDomainApiTestSupport.BASE_TIME.plusSeconds(1), null),
                new ChannelMember(1L, 1003L, ChannelMemberRole.OWNER, MessageDomainApiTestSupport.BASE_TIME.plusSeconds(2), null)
        )));
        fixture.messageRepository.messagesById.put(5002L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5002L, MessageDomainApiTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "member", "member", "member", null, null, "sent", MessageDomainApiTestSupport.BASE_TIME
        ));
        fixture.messageRepository.messagesById.put(5003L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5003L, MessageDomainApiTestSupport.SERVER_ID, 1L, 1L, 1003L, "text", "owner", "owner", "owner", null, null, "sent", MessageDomainApiTestSupport.BASE_TIME
        ));

        ChannelMessageResult recalled = fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5002L));

        assertEquals("recalled", recalled.status());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5003L))
        );

        assertEquals("target member with OWNER role cannot be moderated", exception.getMessage());
    }

    /**
     * 验证 private channel 的 ADMIN 可以撤回 former / non-active 成员消息。
     */
    @Test
    @DisplayName("recall channel message private admin former sender succeeds")
    void recallChannelMessage_privateAdminFormerSender_succeeds() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(1L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                1L,
                1L,
                "project-alpha",
                "",
                "",
                "",
                "private",
                false,
                MessageDomainApiTestSupport.BASE_TIME,
                MessageDomainApiTestSupport.BASE_TIME
        ));
        fixture.channelMemberRepository.memberships.put(1L, new java.util.ArrayList<>(List.of(
                new ChannelMember(1L, 1001L, ChannelMemberRole.ADMIN, MessageDomainApiTestSupport.BASE_TIME, null)
        )));
        fixture.messageRepository.messagesById.put(5004L, new team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage(
                5004L, MessageDomainApiTestSupport.SERVER_ID, 1L, 1L, 1999L, "text", "former member", "former member", "former member", null, null, "sent", MessageDomainApiTestSupport.BASE_TIME
        ));

        ChannelMessageResult recalled = fixture.lifecycleApi.recallChannelMessage(new RecallChannelMessageCommand(1001L, 1L, 5004L));

        assertEquals("recalled", recalled.status());
    }
}
