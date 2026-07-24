package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.math.BigDecimal;
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
    @DisplayName("validate canonical data valid file builds preview and searchable text")
    void validateCanonicalData_validFile_buildsPreviewAndSearchableText() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(true),
                TestFeatureApis.fileReferences()
        );

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of(
                        "text", "项目文档",
                        "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 12345L
                )
        );

        assertEquals("项目文档", canonical.data().get("text"));
        assertEquals("demo.pdf", canonical.data().get("filename"));
        assertEquals("[文件消息] demo.pdf", canonical.preview());
    }

    /**
     * 验证对象不存在时会返回稳定问题语义。
     */
    @Test
    @DisplayName("validate canonical data missing storage object throws not found problem")
    void validateCanonicalData_missingStorageObject_throwsNotFoundProblem() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(false),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "object_key", "channels/1/messages/file/accounts/1001/missing.pdf",
                                "filename", "missing.pdf"
                        )
                )
        );

        assertEquals("storage object does not exist", exception.getMessage());
    }

    /**
     * 验证跨频道范围的 objectKey 会被拒绝。
     */
    @Test
    @DisplayName("validate canonical data out of scope object key throws validation problem")
    void validateCanonicalData_outOfScopeObjectKey_throwsValidationProblem() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(true),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "text", "项目文档",
                                "object_key", "channels/2/messages/file/accounts/demo.pdf",
                                "filename", "demo.pdf",
                                "mime_type", "application/pdf",
                                "size", 12345L
                        )
                )
        );

        assertEquals("file objectKey is out of allowed channel scope", exception.getMessage());
    }

    /**
     * 验证 canonical data 中带小数的 size 不会被截断后写入。
     */
    @Test
    @DisplayName("validate canonical data fractional size throws validation problem")
    void validateCanonicalData_fractionalSize_throwsValidationProblem() {
        FileChannelMessagePlugin plugin = new FileChannelMessagePlugin(
                storageService(true),
                TestFeatureApis.fileReferences()
        );

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of(
                                "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                                "filename", "demo.pdf",
                                "size", new BigDecimal("12.5")
                        )
                )
        );

        assertEquals("size must be integer", exception.getMessage());
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
