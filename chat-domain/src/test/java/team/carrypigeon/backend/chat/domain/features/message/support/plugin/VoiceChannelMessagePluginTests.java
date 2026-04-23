package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
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
 * VoiceChannelMessagePlugin 契约测试。
 * 职责：验证语音消息插件的预览、检索与语音元数据校验语义。
 * 边界：不访问真实对象存储，只验证 storage-api 抽象交互后的稳定行为。
 */
class VoiceChannelMessagePluginTests {

    /**
     * 验证语音消息插件会生成稳定的语音预览与检索文本。
     */
    @Test
    @DisplayName("create message valid voice draft builds preview and searchable text")
    void createMessage_validVoiceDraft_buildsPreviewAndSearchableText() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new VoiceChannelMessageDraft(null, "channels/1/messages/voice/accounts/1001/5001-demo.mp3", "demo.mp3", "audio/mpeg", 45678L, 12000L, "会议纪要", null)
        );

        assertEquals("voice", message.messageType());
        assertEquals("会议纪要", message.body());
        assertEquals("[语音消息] demo.mp3 12s", message.previewText());
        assertEquals("会议纪要 demo.mp3", message.searchableText());
    }

    /**
     * 验证无效时长会返回稳定校验问题。
     */
    @Test
    @DisplayName("create message invalid duration throws validation problem")
    void createMessage_invalidDuration_throwsValidationProblem() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new VoiceChannelMessageDraft(null, "channels/1/messages/voice/accounts/1001/5001-demo.mp3", "demo.mp3", null, null, 0L, null, null)
                )
        );

        assertEquals("durationMillis must be greater than 0", exception.getMessage());
    }

    /**
     * 验证跨频道范围的语音 objectKey 会被拒绝。
     */
    @Test
    @DisplayName("create message out of scope voice object key throws validation problem")
    void createMessage_outOfScopeVoiceObjectKey_throwsValidationProblem() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                new JsonProvider(new ObjectMapper()),
                new MessageAttachmentObjectKeyPolicy()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new VoiceChannelMessageDraft(null, "channels/2/messages/voice/accounts/1001/5001-demo.mp3", "demo.mp3", "audio/mpeg", 45678L, 12000L, "会议纪要", null)
                )
        );

        assertEquals("voice objectKey is out of allowed channel scope", exception.getMessage());
    }

    private static ObjectStorageService storageService() {
        return new ObjectStorageService() {
            @Override
            public StorageObject put(PutObjectCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<StorageObject> get(GetObjectCommand command) {
                return Optional.of(StorageObject.metadata(command.objectKey(), "audio/mpeg", 45678L));
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
