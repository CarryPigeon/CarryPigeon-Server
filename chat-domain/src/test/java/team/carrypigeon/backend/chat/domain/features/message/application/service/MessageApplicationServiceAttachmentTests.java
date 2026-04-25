package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UploadChannelMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageApplicationService 附件契约测试。
 * 职责：验证附件上传、附件消息发送与附件历史读取的应用层编排契约。
 * 边界：不验证 HTTP、Netty 和真实外部存储，只使用测试对象存储替身验证附件相关语义。
 */
@Tag("contract")
class MessageApplicationServiceAttachmentTests {

    /**
     * 验证 file 消息发送结果会派生临时访问 URL，同时持久化消息仍保持 canonical payload。
     */
    @Test
    @DisplayName("send channel message file draft returns payload with access url")
    void sendChannelMessage_fileDraft_returnsPayloadWithAccessUrl() {
        MessageApplicationServiceTestSupport.TestObjectStorageService storageService = new MessageApplicationServiceTestSupport.TestObjectStorageService();
        storageService.getResult = Optional.of(StorageObject.metadata("channels/1/messages/file/accounts/1001/5001-demo.pdf", "application/pdf", 1024L));
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1800)
        );
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(storageService);

        ChannelMessageResult result = fixture.service.sendChannelMessage(new SendChannelMessageCommand(
                1001L,
                1L,
                new FileChannelMessageDraft(
                        "项目文档",
                        "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "demo.pdf",
                        "application/pdf",
                        1024L,
                        null
                )
        ));

        assertEquals("file", result.messageType());
        assertEquals(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                fixture.jsonProvider.readTree(result.payload()).path("object_key").asText()
        );
        assertEquals(
                "http://127.0.0.1:9000/file/demo.pdf?token=abc",
                fixture.jsonProvider.readTree(result.payload()).path("access_url").asText()
        );
        assertEquals(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                fixture.jsonProvider.readTree(fixture.messageRepository.savedMessages.getFirst().payload()).path("object_key").asText()
        );
        assertTrue(fixture.jsonProvider.readTree(fixture.messageRepository.savedMessages.getFirst().payload()).path("access_url").isMissingNode());
    }

    /**
     * 验证附件上传成功后会返回 canonical objectKey 与存储元信息。
     */
    @Test
    @DisplayName("upload channel message attachment valid command returns canonical attachment result")
    void uploadChannelMessageAttachment_validCommand_returnsCanonicalAttachmentResult() {
        MessageApplicationServiceTestSupport.TestObjectStorageService storageService = new MessageApplicationServiceTestSupport.TestObjectStorageService();
        storageService.putResult = StorageObject.metadata(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                "application/pdf",
                123L
        );
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(storageService);

        ChannelMessageAttachmentUploadResult result = fixture.service.uploadChannelMessageAttachment(
                new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "file",
                        "demo.pdf",
                        "application/pdf",
                        123L,
                        new ByteArrayInputStream("demo-content".getBytes())
                )
        );

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", result.objectKey());
        assertEquals("demo.pdf", result.filename());
        assertEquals("application/pdf", result.mimeType());
        assertEquals(123L, result.size());
        assertNotNull(storageService.lastPutCommand);
        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", storageService.lastPutCommand.objectKey());
    }

    /**
     * 验证非法 messageType 会被识别为参数错误。
     */
    @Test
    @DisplayName("upload channel message attachment invalid type throws validation problem")
    void uploadChannelMessageAttachment_invalidType_throwsValidationProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(new MessageApplicationServiceTestSupport.TestObjectStorageService());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.uploadChannelMessageAttachment(new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "image",
                        "demo.png",
                        "image/png",
                        64L,
                        new ByteArrayInputStream(new byte[]{1, 2, 3})
                ))
        );

        assertEquals("messageType must be file or voice", exception.getMessage());
    }

    /**
     * 验证非成员上传附件时会返回权限问题语义。
     */
    @Test
    @DisplayName("upload channel message attachment non member throws forbidden problem")
    void uploadChannelMessageAttachment_nonMember_throwsForbiddenProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(new MessageApplicationServiceTestSupport.TestObjectStorageService());
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.uploadChannelMessageAttachment(new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "file",
                        "demo.pdf",
                        "application/pdf",
                        64L,
                        new ByteArrayInputStream(new byte[]{1, 2, 3})
                ))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证 file 插件未启用时上传前校验会阻断附件链路。
     */
    @Test
    @DisplayName("upload channel message attachment file plugin disabled throws validation problem")
    void uploadChannelMessageAttachment_filePluginDisabled_throwsValidationProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.uploadChannelMessageAttachment(new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "file",
                        "demo.txt",
                        "text/plain",
                        4L,
                        new ByteArrayInputStream("demo".getBytes())
                ))
        );

        assertEquals("message type is not enabled", exception.getMessage());
    }

    /**
     * 验证 file 历史消息读取时会派生临时访问 URL。
     */
    @Test
    @DisplayName("get channel message history file message returns payload with access url")
    void getChannelMessageHistory_fileMessage_returnsPayloadWithAccessUrl() {
        MessageApplicationServiceTestSupport.TestObjectStorageService storageService = new MessageApplicationServiceTestSupport.TestObjectStorageService();
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1800)
        );
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(storageService);
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L,
                "carrypigeon-local",
                1L,
                1L,
                1002L,
                "file",
                "项目文档",
                "[文件消息] demo.pdf",
                "项目文档 demo.pdf",
                fixture.jsonProvider.toJson(Map.of(
                        "object_key", "channels/1/messages/file/accounts/1002/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 1024L
                )),
                null,
                "sent",
                MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1)
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals(
                "http://127.0.0.1:9000/file/demo.pdf?token=abc",
                fixture.jsonProvider.readTree(result.messages().getFirst().payload()).path("access_url").asText()
        );
        assertEquals(
                "channels/1/messages/file/accounts/1002/5001-demo.pdf",
                fixture.jsonProvider.readTree(result.messages().getFirst().payload()).path("object_key").asText()
        );
    }
}
