package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageHttpCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.CustomChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 消息发送契约测试。
 * 职责：验证文本消息发送入口的应用层编排契约与权限失败语义。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证发送相关语义。
 */
@Tag("contract")
class ChannelMessagePublishingDomainApiTests {

    /**
     * 验证发送文本消息时会持久化并使用同一个 messageId 进行实时分发。
     */
    @Test
    @DisplayName("send channel text message valid command persists and publishes same message id")
    void sendChannelTextMessage_validCommand_persistsAndPublishesSameMessageId() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelTextMessage(
                new SendChannelTextMessageCommand(1001L, 1L, "hello world")
        );

        assertEquals(5001L, result.messageId());
        assertEquals(5001L, fixture.messageRepository.savedMessages.getFirst().messageId());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.serverId());
        assertEquals("hello world", result.body());
        assertEquals("hello world", result.previewText());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证 `sendChannelMessage` 在 `userMention` 条件下满足 `persistsMentionAndPublishesRealtimeMention` 的测试契约。
     */
    @Test
    @DisplayName("send channel message user mention persists mention and publishes realtime mention")
    void sendChannelMessage_userMention_persistsMentionAndPublishesRealtimeMention() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessageHttp(
                new SendChannelMessageHttpCommand(
                        1001L,
                        1L,
                        "Core:Text",
                        "1.0.0",
                        java.util.Map.of("text", "hello @carry-user"),
                        null,
                        List.of(new EditChannelMessageCommand.MentionTargetCommand("user", 1002L)),
                        null
                )
        );

        assertEquals(5001L, result.messageId());
        assertEquals(1, fixture.mentionRepository.mentions.size());
        assertEquals(1, fixture.publisher.createdMentions.size());
        assertEquals(1002L, fixture.publisher.createdMentions.getFirst().targetAccountId());
        assertEquals(5001L, fixture.publisher.createdMentions.getFirst().messageId());
    }

    /**
     * 验证 file HTTP 消息可以直接引用附件上传返回的 object_key。
     */
    @Test
    @DisplayName("send channel message file object key persists attachment payload")
    void sendChannelMessage_fileObjectKey_persistsAttachmentPayload() {
        MessageDomainApiTestSupport.TestObjectStorageService storageService =
                new MessageDomainApiTestSupport.TestObjectStorageService();
        storageService.getResult = java.util.Optional.of(
                team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject.metadata(
                        "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "application/pdf",
                        123L
                )
        );
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(storageService);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessageHttp(
                new SendChannelMessageHttpCommand(
                        1001L,
                        1L,
                        "Core:File",
                        "1.0.0",
                        java.util.Map.of(
                                "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                                "filename", "demo.pdf",
                                "mime_type", "application/pdf",
                                "size", 123L
                        ),
                        null,
                        List.of(),
                        null
                )
        );

        assertEquals("file", result.messageType());
        assertTrue(fixture.messageRepository.savedMessages.getFirst().payload()
                .contains("\"object_key\":\"channels/1/messages/file/accounts/1001/5001-demo.pdf\""));
        assertTrue(result.payload().contains("\"share_key\":\"shr_att_"));
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
    }

    /**
     * 验证附件上传会写入对象存储并返回后续消息发送可用的稳定引用。
     */
    @Test
    @DisplayName("upload message attachment stores object and returns stable reference")
    void uploadMessageAttachment_storesObjectAndReturnsStableReference() {
        MessageDomainApiTestSupport.TestObjectStorageService storageService =
                new MessageDomainApiTestSupport.TestObjectStorageService();
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(storageService);

        var result = fixture.attachmentApi.uploadMessageAttachment(
                1001L,
                1L,
                "file",
                "demo.pdf",
                "application/pdf",
                4L,
                new ByteArrayInputStream("demo".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", result.objectKey());
        assertTrue(result.shareKey().startsWith("shr_att_"));
        assertEquals("demo.pdf", result.filename());
        assertEquals("application/pdf", result.mimeType());
        assertEquals(4L, result.size());
        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", storageService.lastPutCommand.objectKey());
    }

    /**
     * 验证 mention 持久化中途失败时，不会提前广播已创建的 mention 事件。
     */
    @Test
    @DisplayName("send channel message mention persistence failure does not publish partial mention events")
    void sendChannelMessage_mentionPersistenceFailure_doesNotPublishPartialMentionEvents() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelMemberRepository.save(new ChannelMember(1L, 1003L, ChannelMemberRole.MEMBER, MessageDomainApiTestSupport.BASE_TIME.plusSeconds(2), null));
        fixture.mentionRepository.failOnSaveCall = 2;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fixture.publishingApi.sendChannelMessageHttp(
                        new SendChannelMessageHttpCommand(
                                1001L,
                                1L,
                                "Core:Text",
                                "1.0.0",
                                java.util.Map.of("text", "hello @carry-user and @carry-ops"),
                                null,
                                List.of(
                                        new EditChannelMessageCommand.MentionTargetCommand("user", 1002L),
                                        new EditChannelMessageCommand.MentionTargetCommand("user", 1003L)
                                ),
                                null
                        )
                )
        );

        assertEquals("mention persistence failed", exception.getMessage());
        assertEquals(0, fixture.publisher.createdMentions.size());
    }

    /**
     * 验证事务回滚时，不会发送消息广播或 mention 广播。
     */
    @Test
    @DisplayName("send channel message rollback skips message and mention publish")
    void sendChannelMessage_rollback_skipsMessageAndMentionPublish() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(
                null,
                new MessageDomainApiTestSupport.RollbackingTransactionRunner()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fixture.publishingApi.sendChannelMessageHttp(
                        new SendChannelMessageHttpCommand(
                                1001L,
                                1L,
                                "Core:Text",
                                "1.0.0",
                                java.util.Map.of("text", "hello @carry-user"),
                                null,
                                List.of(new EditChannelMessageCommand.MentionTargetCommand("user", 1002L)),
                                null
                        )
                )
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, fixture.publisher.publishedMessages.size());
        assertEquals(0, fixture.publisher.createdMentions.size());
    }

    /**
     * 验证通用消息发送入口在 text 草稿场景下保持既有 text 消息语义。
     */
    @Test
    @DisplayName("send channel message text draft preserves text semantics")
    void sendChannelMessage_textDraft_preservesTextSemantics() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(
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
     * 验证 plugin 草稿会走通当前通用发送主链路。
     */
    @Test
    @DisplayName("send channel message plugin draft persists plugin message")
    void sendChannelMessage_pluginDraft_persistsPluginMessage() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(
                new SendChannelMessageCommand(1001L, 1L, new PluginChannelMessageDraft(
                        "test-extension",
                        "mc bridge",
                        "test-extension",
                        "{\"event\":\"player_join\"}",
                        null
                ))
        );

        assertEquals("test-extension", result.messageType());
        assertEquals("mc bridge", result.body());
        assertEquals("[插件消息] mc bridge", result.previewText());
    }

    /**
     * 验证 custom 草稿会走通当前通用发送主链路。
     */
    @Test
    @DisplayName("send channel message custom draft persists custom message")
    void sendChannelMessage_customDraft_persistsCustomMessage() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(
                new SendChannelMessageCommand(1001L, 1L, new CustomChannelMessageDraft(
                        "status card",
                        "{\"card\":\"server-status\"}",
                        null
                ))
        );

        assertEquals("custom", result.messageType());
        assertEquals("status card", result.body());
        assertEquals("[自定义消息] status card", result.previewText());
    }

    /**
     * 验证内部 system 消息发送会在 system 频道中持久化并实时分发。
     */
    @Test
    @DisplayName("send system channel message valid command persists and publishes system message")
    void sendSystemChannelMessage_validCommand_persistsAndPublishesSystemMessage() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(2L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                2L,
                2L,
                "system-announcement",
                "",
                "",
                "",
                "system",
                false,
                MessageDomainApiTestSupport.BASE_TIME,
                MessageDomainApiTestSupport.BASE_TIME
        ));

        ChannelMessageResult result = fixture.publishingApi.sendSystemChannelMessage(
                new SendSystemChannelMessageCommand(1001L, 2L, "maintenance notice", "{\"severity\":\"info\"}", null)
        );

        assertEquals("system", result.messageType());
        assertEquals("maintenance notice", result.body());
        assertEquals("[系统消息] maintenance notice", result.previewText());
    }

    /**
     * 验证非 system 频道不能发送 system 消息。
     */
    @Test
    @DisplayName("send system channel message non system channel throws forbidden problem")
    void sendSystemChannelMessage_nonSystemChannel_throwsForbiddenProblem() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.publishingApi.sendSystemChannelMessage(
                        new SendSystemChannelMessageCommand(1001L, 1L, "maintenance notice", "{\"severity\":\"info\"}", null)
                )
        );

        assertEquals("system message requires system channel", exception.getMessage());
    }

    /**
     * 验证非成员发送消息时会返回权限问题语义。
     */
    @Test
    @DisplayName("send channel text message non member throws forbidden problem")
    void sendChannelTextMessage_nonMember_throwsForbiddenProblem() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.publishingApi.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证文本消息发送入口会先拒绝空正文。
     */
    @Test
    @DisplayName("send channel text message blank body throws validation problem")
    void sendChannelTextMessage_blankBody_throwsValidationProblem() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.publishingApi.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, " "))
        );

        assertEquals("body must not be blank", exception.getMessage());
    }

    /**
     * 验证 private channel 成员被禁言时无法继续发送消息。
     */
    @Test
    @DisplayName("send channel text message muted private member throws forbidden problem")
    void sendChannelTextMessage_mutedPrivateMember_throwsForbiddenProblem() {
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
        fixture.channelMemberRepository.memberships.put(1L, List.of(
                new ChannelMember(
                        1L,
                        1001L,
                        ChannelMemberRole.MEMBER,
                        MessageDomainApiTestSupport.BASE_TIME,
                        MessageDomainApiTestSupport.BASE_TIME.plusSeconds(300)
                )
        ));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.publishingApi.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel member is muted", exception.getMessage());
    }

}
