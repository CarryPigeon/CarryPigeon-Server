package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.support.TestFeatureApis;
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
@Tag("contract")
class VoiceChannelMessagePluginTests {

    /**
     * 验证语音消息插件会生成稳定的语音预览与检索文本。
     */
    @Test
    @DisplayName("validate canonical data valid voice builds preview and searchable text")
    void validateCanonicalData_validVoice_buildsPreviewAndSearchableText() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                TestFeatureApis.fileReferences()
        );

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of(
                        "object_key", "channels/1/messages/voice/accounts/1001/5001-demo.mp3",
                        "filename", "demo.mp3",
                        "mime_type", "audio/mpeg",
                        "size", 45678L,
                        "duration_millis", 12000L,
                        "transcript", "会议纪要"
                )
        );

        assertEquals("会议纪要", canonical.data().get("transcript"));
        assertEquals("[语音消息] demo.mp3 12s", canonical.preview());
    }

    /**
     * 验证无效时长会返回稳定校验问题。
     */
    @Test
    @DisplayName("validate canonical data invalid duration throws validation problem")
    void validateCanonicalData_invalidDuration_throwsValidationProblem() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "object_key", "channels/1/messages/voice/accounts/1001/5001-demo.mp3",
                                "filename", "demo.mp3",
                                "duration_millis", 0L
                        )
                )
        );

        assertEquals("duration_millis must be greater than 0", exception.getMessage());
    }

    /**
     * 验证跨频道范围的语音 objectKey 会被拒绝。
     */
    @Test
    @DisplayName("validate canonical data out of scope voice object key throws validation problem")
    void validateCanonicalData_outOfScopeVoiceObjectKey_throwsValidationProblem() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "object_key", "channels/2/messages/voice/accounts/1001/5001-demo.mp3",
                                "filename", "demo.mp3",
                                "mime_type", "audio/mpeg",
                                "size", 45678L,
                                "duration_millis", 12000L,
                                "transcript", "会议纪要"
                        )
                )
        );

        assertEquals("voice objectKey is out of allowed channel scope", exception.getMessage());
    }

    /**
     * 验证 canonical data 中非整数时长会被插件拒绝。
     */
    @Test
    @DisplayName("validate canonical data decimal duration string throws validation problem")
    void validateCanonicalData_decimalDurationString_throwsValidationProblem() {
        VoiceChannelMessagePlugin plugin = new VoiceChannelMessagePlugin(
                storageService(),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "object_key", "channels/1/messages/voice/accounts/1001/5001-demo.mp3",
                                "filename", "demo.mp3",
                                "duration_millis", "12.5"
                        )
                )
        );

        assertEquals("duration_millis must be integer", exception.getMessage());
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
