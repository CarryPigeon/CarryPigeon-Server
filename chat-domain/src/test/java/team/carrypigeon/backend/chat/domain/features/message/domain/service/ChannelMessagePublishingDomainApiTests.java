package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息发送契约测试。
 * 职责：验证 canonical 文本、回复、附件、mentions 与系统消息的创建语义。
 * 边界：不验证 HTTP、Netty 和真实数据库访问。
 */
@Tag("contract")
class ChannelMessagePublishingDomainApiTests {

    /**
     * 验证文本消息使用统一 domain envelope 持久化和广播。
     */
    @Test
    @DisplayName("send text message persists canonical envelope")
    void sendChannelMessage_validTextData_persistsCanonicalEnvelope() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                1001L, 1L, "Core:Text", "1.0.0", Map.of("text", "hello world"), List.of(), null
        ));

        assertEquals(5001L, result.messageId());
        assertEquals("Core:Text", result.domain());
        assertEquals("1.0.0", result.domainVersion());
        assertEquals(Map.of("text", "hello world"), result.data());
        assertEquals("hello world", result.preview());
        assertEquals(MessageStatus.SENT, result.status());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证 mentions 去重保序并生成派生提醒索引。
     */
    @Test
    @DisplayName("send canonical message normalizes mentions")
    void sendChannelMessage_duplicateMentions_normalizesMentions() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                1001L, 1L, "Core:Text", "1.0.0", Map.of("text", "hello"), List.of(1002L, 1002L), null
        ));

        assertEquals(List.of(1002L), result.mentions());
        assertEquals(1, fixture.mentionRepository.mentions.size());
        assertEquals(1002L, fixture.mentionRepository.mentions.getFirst().targetAccountId());
        assertEquals(1, fixture.publisher.createdMentions.size());
    }

    /**
     * 验证 data 内的 mentions 会被拒绝，避免形成第二个提醒元数据真源。
     */
    @Test
    @DisplayName("send canonical message with mentions in data fails validation")
    void sendChannelMessage_mentionsInsideData_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:Text", "1.0.0",
                        Map.of("text", "hello", "mentions", List.of("1002")), List.of(), null
                ))
        );

        assertEquals("mentions must be top-level message metadata", exception.getMessage());
    }

    /**
     * 验证 ReplyText 的关系字段保留在 data 内。
     */
    @Test
    @DisplayName("send reply text keeps relation fields in data")
    void sendChannelMessage_replyText_keepsRelationsInData() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        Map<String, Object> data = Map.of(
                "content", Map.of("text", "reply"),
                "reply_to_mid", "4999",
                "reply_to", Map.of("mid", "4999", "sender_name", "Bob", "preview", "source", "created_at", 1L)
        );

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                1001L, 1L, "Core:ReplyText", "1.0.0", data, List.of(), null
        ));

        assertEquals("Core:ReplyText", result.domain());
        assertEquals("4999", result.data().get("reply_to_mid"));
        assertEquals("reply", result.preview());
    }

    /**
     * 验证 ReplyText 缺少回复和引用锚点时拒绝创建。
     */
    @Test
    @DisplayName("send reply text without anchor fails validation")
    void sendChannelMessage_replyTextWithoutAnchor_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:ReplyText", "1.0.0",
                        Map.of("content", Map.of("text", "reply")), List.of(), null
                ))
        );

        assertEquals("reply_to_mid or quote_reply is required", exception.getMessage());
    }

    /**
     * 验证 ReplyText 的 Snowflake ID 必须按 Wire 约定使用十进制字符串。
     */
    @Test
    @DisplayName("send reply text with numeric id fails validation")
    void sendChannelMessage_replyTextWithNumericId_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:ReplyText", "1.0.0",
                        Map.of("content", Map.of("text", "reply"), "reply_to_mid", 4999L), List.of(), null
                ))
        );

        assertEquals("reply_to_mid must be decimal snowflake string", exception.getMessage());
    }

    /**
     * 验证未知 domain 不会绕过插件注册表写入消息或发布事件。
     */
    @Test
    @DisplayName("send canonical message with unknown domain fails before persistence")
    void sendChannelMessage_unknownDomain_failsBeforePersistence() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Example:Missing", "1.0.0", Map.of("value", "unsafe"), List.of(), null
                ))
        );

        assertEquals("domain is not supported", exception.getMessage());
        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
        assertTrue(fixture.publisher.publishedMessages.isEmpty());
    }

    /**
     * 验证 domain 版本由对应插件校验，错误版本不会进入持久化。
     */
    @Test
    @DisplayName("send canonical message with unsupported version fails before persistence")
    void sendChannelMessage_unsupportedVersion_failsBeforePersistence() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:Text", "2.0.0", Map.of("text", "hello"), List.of(), null
                ))
        );

        assertEquals("domain version is not supported", exception.getMessage());
        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
        assertTrue(fixture.publisher.publishedMessages.isEmpty());
    }

    /**
     * 验证内建插件严格校验字段 JSON 类型，异常 data 不会被存储。
     */
    @Test
    @DisplayName("send text with non string body fails before persistence")
    void sendChannelMessage_nonStringText_failsBeforePersistence() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:Text", "1.0.0", Map.of("text", 42), List.of(), null
                ))
        );

        assertEquals("text must be string", exception.getMessage());
        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
        assertTrue(fixture.publisher.publishedMessages.isEmpty());
    }

    /**
     * 验证已注册扩展 domain 可通过 HTTP 插件校验链路创建 canonical 消息。
     */
    @Test
    @DisplayName("send registered extension domain persists plugin validated data")
    void sendChannelMessage_registeredExtension_persistsPluginValidatedData() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        Map<String, Object> payload = Map.of("event", "player_join", "level", 3);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(
                new SendChannelMessageCommand(
                        1001L,
                        1L,
                        "test-extension",
                        "1.0.0",
                        Map.of("plugin_key", "test-extension", "payload", payload, "text", "joined"),
                        List.of(),
                        null
                )
        );

        assertEquals("test-extension", result.domain());
        assertEquals(payload, result.data().get("payload"));
        assertEquals("[插件消息] joined", result.preview());
        assertEquals(1, fixture.messageRepository.savedMessages.size());
        assertEquals(1, fixture.publisher.publishedMessages.size());
    }

    /**
     * 验证扩展插件拒绝不符合自身 schema 的 payload，且不产生消息副作用。
     */
    @Test
    @DisplayName("send extension with malformed payload fails before persistence")
    void sendChannelMessage_extensionMalformedPayload_failsBeforePersistence() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L,
                        1L,
                        "test-extension",
                        "1.0.0",
                        Map.of("plugin_key", "test-extension", "payload", "not-an-object"),
                        List.of(),
                        null
                ))
        );

        assertEquals("payload must be object", exception.getMessage());
        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
        assertTrue(fixture.publisher.publishedMessages.isEmpty());
    }

    /**
     * 验证文件消息将稳定文件引用写入 canonical data。
     */
    @Test
    @DisplayName("send file message persists canonical attachment data")
    void sendChannelMessage_file_persistsCanonicalData() {
        MessageDomainApiTestSupport.TestObjectStorageService storageService =
                new MessageDomainApiTestSupport.TestObjectStorageService();
        storageService.getResult = java.util.Optional.of(StorageObject.metadata(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf", "application/pdf", 123L
        ));
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(storageService);

        ChannelMessageResult result = fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                1001L,
                1L,
                "Core:File",
                "1.0.0",
                Map.of(
                        "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 123L
                ),
                List.of(),
                null
        ));

        assertEquals("Core:File", result.domain());
        assertTrue(String.valueOf(result.data().get("share_key")).startsWith("shr_att_"));
        assertEquals("demo.pdf", result.data().get("filename"));
    }

    /**
     * 验证附件上传仍返回后续 canonical 文件消息可用的稳定引用。
     */
    @Test
    @DisplayName("upload attachment returns stable reference")
    void uploadMessageAttachment_validFile_returnsStableReference() {
        MessageDomainApiTestSupport.TestObjectStorageService storageService =
                new MessageDomainApiTestSupport.TestObjectStorageService();
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(storageService);

        var result = fixture.attachmentApi.uploadMessageAttachment(
                1001L, 1L, "file", "demo.pdf", "application/pdf", 4L,
                new ByteArrayInputStream("demo".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", result.objectKey());
        assertTrue(result.shareKey().startsWith("shr_att_"));
    }

    /**
     * 验证内部系统消息也使用 canonical envelope。
     */
    @Test
    @DisplayName("send system message persists canonical envelope")
    void sendSystemChannelMessage_systemChannel_persistsCanonicalEnvelope() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(2L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                2L, 2L, "system", "", "", "", "system", false,
                MessageDomainApiTestSupport.BASE_TIME, MessageDomainApiTestSupport.BASE_TIME
        ));

        ChannelMessageResult result = fixture.publishingApi.sendSystemChannelMessage(
                new SendSystemChannelMessageCommand(
                        1001L, 2L, "1.0.0", Map.of("text", "maintenance", "severity", "info"), List.of()
                )
        );

        assertEquals("Core:System", result.domain());
        assertEquals("maintenance", result.data().get("text"));
        assertEquals(MessageStatus.SENT, result.status());
    }

    /**
     * 验证客户端发送入口不能借 canonical envelope 伪造内部系统消息。
     */
    @Test
    @DisplayName("send system domain through client entry fails validation")
    void sendChannelMessage_systemDomain_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L,
                        1L,
                        "Core:System",
                        "1.0.0",
                        Map.of("text", "forged system message"),
                        List.of(),
                        null
                ))
        );

        assertEquals("domain is not client-sendable", exception.getMessage());
        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
    }

    /**
     * 验证非成员不能向频道写入消息。
     */
    @Test
    @DisplayName("send text message by non member fails permission")
    void sendChannelMessage_nonMember_failsPermission() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.sendChannelMessage(new SendChannelMessageCommand(
                        1001L, 1L, "Core:Text", "1.0.0", Map.of("text", "hello"), List.of(), null
                ))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }
}
