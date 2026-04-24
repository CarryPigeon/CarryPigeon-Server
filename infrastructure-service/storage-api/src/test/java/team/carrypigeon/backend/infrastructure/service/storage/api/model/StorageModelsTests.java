package team.carrypigeon.backend.infrastructure.service.storage.api.model;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证对象存储 API 模型的最小契约。
 * 职责：确保对象键、TTL 和对象模型遵守稳定输入约束。
 * 边界：不验证 MinIO 行为，只验证 API 值对象和命令自身语义。
 */
@Tag("unit")
class StorageModelsTests {

    /**
     * 测试合法对象命令和结果模型。
     * 输入：合法对象键、正数大小和正数 TTL。
     * 期望：对象模型创建成功并保留关键信息。
     */
    @Test
    void constructor_validModels_keepValues() {
        PutObjectCommand putCommand = new PutObjectCommand(
                "files/a.txt",
                new ByteArrayInputStream("hello".getBytes()),
                5,
                "text/plain"
        );
        PresignedUrlCommand urlCommand = new PresignedUrlCommand("files/a.txt", Duration.ofMinutes(10));
        StorageObject storageObject = StorageObject.metadata("files/a.txt", "text/plain", 5);
        PresignedUrl presignedUrl = new PresignedUrl(java.net.URI.create("https://example.com/a.txt"), Instant.now().plusSeconds(60));

        assertEquals("files/a.txt", putCommand.objectKey());
        assertEquals(Duration.ofMinutes(10), urlCommand.ttl());
        assertEquals("files/a.txt", storageObject.objectKey());
        assertEquals("https://example.com/a.txt", presignedUrl.url().toString());
    }

    /**
     * 测试非法对象键和 TTL。
     * 输入：空白对象键或非正 TTL。
     * 期望：命令对象拒绝创建，避免非法对象存储契约流入实现层。
     */
    @Test
    void constructor_invalidObjectKeyOrTtl_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new GetObjectCommand(" "));
        assertThrows(IllegalArgumentException.class, () -> new PresignedUrlCommand("files/a.txt", Duration.ZERO));
    }
}
