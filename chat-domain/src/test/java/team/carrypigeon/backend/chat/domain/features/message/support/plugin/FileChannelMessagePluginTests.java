package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FileChannelMessagePlugin 契约测试。
 * 职责：验证文件消息插件的预览、检索与对象引用校验语义。
 * 边界：不访问真实对象存储，只验证 storage-api 抽象交互后的稳定行为。
 */
@Tag("contract")
class FileChannelMessagePluginTests {

    /**
     * 验证文件消息插件会生成稳定的文件预览与检索文本。
     */
    @Test
    @DisplayName("create message valid file draft builds preview and searchable text")
    void createMessage_validFileDraft_buildsPreviewAndSearchableText() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(true),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new FileChannelMessageDraft("项目文档", "channels/1/messages/file/accounts/1001/5001-demo.pdf", "demo.pdf", "application/pdf", 12345L, null)
        );

        assertEquals("file", message.messageType());
        assertEquals("项目文档", message.body());
        assertEquals("[文件消息] demo.pdf", message.previewText());
        assertEquals("项目文档 demo.pdf", message.searchableText());
    }

    /**
     * 验证对象不存在时会返回稳定问题语义。
     */
    @Test
    @DisplayName("create message missing storage object throws not found problem")
    void createMessage_missingStorageObject_throwsNotFoundProblem() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(false),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new FileChannelMessageDraft(null, "channels/1/messages/file/accounts/1001/missing.pdf", "missing.pdf", null, null, null)
                )
        );

        assertEquals("storage object does not exist", exception.getMessage());
    }

    /**
     * 验证跨频道范围的 objectKey 会被拒绝。
     */
    @Test
    @DisplayName("create message out of scope object key throws validation problem")
    void createMessage_outOfScopeObjectKey_throwsValidationProblem() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(true),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new FileChannelMessageDraft("项目文档", "channels/2/messages/file/accounts/demo.pdf", "demo.pdf", "application/pdf", 12345L, null)
                )
        );

        assertEquals("file objectKey is out of allowed channel scope", exception.getMessage());
    }

    private static ObjectStorageService storageService(boolean exists) {
        return new ObjectStorageService() {
            @Override
            public StorageObject put(PutObjectCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<StorageObject> get(GetObjectCommand command) {
                if (!exists) {
                    return Optional.empty();
                }
                return Optional.of(StorageObject.metadata(command.objectKey(), "application/pdf", 12345L));
            }

            @Override
            public void delete(team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
