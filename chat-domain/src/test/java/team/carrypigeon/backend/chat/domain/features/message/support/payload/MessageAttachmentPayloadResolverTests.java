package team.carrypigeon.backend.chat.domain.features.message.support.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageAttachmentPayloadResolver 契约测试。
 * 职责：验证 file / voice 消息在出站阶段的附件访问字段派生语义。
 * 边界：不验证真实对象存储访问，只验证 payload 解析和回退行为。
 */
@Tag("contract")
class MessageAttachmentPayloadResolverTests {

    /**
     * 验证 file payload 会收口为 share_key 与相对下载路径。
     */
    @Test
    @DisplayName("resolve file payload rewrites object key to share key fields")
    void resolve_filePayload_rewritesObjectKeyToShareKeyFields() {
        JsonProvider jsonProvider = jsonProvider();
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider);
        String payload = jsonProvider.toJson(Map.of(
                "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                "filename", "demo.pdf",
                "mime_type", "application/pdf",
                "size", 1024L
        ));

        String resolved = resolver.resolve("file", payload);

        assertTrue(jsonProvider.readTree(resolved).path("object_key").isMissingNode());
        assertEquals("shr_att_Y2hhbm5lbHMvMS9tZXNzYWdlcy9maWxlL2FjY291bnRzLzEwMDEvNTAwMS1kZW1vLnBkZg", jsonProvider.readTree(resolved).path("share_key").asText());
        assertEquals("api/files/download/shr_att_Y2hhbm5lbHMvMS9tZXNzYWdlcy9maWxlL2FjY291bnRzLzEwMDEvNTAwMS1kZW1vLnBkZg", jsonProvider.readTree(resolved).path("download_path").asText());
    }

    /**
     * 验证 text 消息不会被错误附加附件访问字段。
     */
    @Test
    @DisplayName("resolve text payload returns original payload")
    void resolve_textPayload_returnsOriginalPayload() {
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider());
        String payload = "plain-text-payload";

        String resolved = resolver.resolve("text", payload);

        assertEquals(payload, resolved);
    }

    /**
     * 验证非法 payload 不会破坏读取链路，而是回退原始值。
     */
    @Test
    @DisplayName("resolve malformed payload returns original payload")
    void resolve_malformedPayload_returnsOriginalPayload() {
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(objectProvider(new TestObjectStorageService()), jsonProvider());

        String resolved = resolver.resolve("file", "not-json");

        assertEquals("not-json", resolved);
    }

    /**
     * 验证 `resolve` 在 `shareKeyPayload` 条件下满足 `keepsStableExternalFields` 的测试契约。
     */
    @Test
    @DisplayName("resolve share key payload keeps stable external fields")
    void resolve_shareKeyPayload_keepsStableExternalFields() {
        JsonProvider jsonProvider = jsonProvider();
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider);
        String payload = jsonProvider.toJson(Map.of(
                "share_key", "shr_7001",
                "filename", "demo.pdf",
                "mime_type", "application/pdf"
        ));

        String resolved = resolver.resolve("file", payload);

        assertEquals("shr_7001", jsonProvider.readTree(resolved).path("share_key").asText());
        assertEquals("api/files/download/shr_7001", jsonProvider.readTree(resolved).path("download_path").asText());
    }

    private static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }

    private static ObjectProvider<ObjectStorageService> objectProvider(ObjectStorageService storageService) {
        return new ObjectProvider<>() {
            @Override
            public ObjectStorageService getObject(Object... args) {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfAvailable() {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfUnique() {
                return storageService;
            }

            @Override
            public ObjectStorageService getObject() {
                return storageService;
            }
        };
    }

    /**
     * `TestObjectStorageService` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class TestObjectStorageService implements ObjectStorageService {

        @Override
        public StorageObject put(PutObjectCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            return Optional.empty();
        }

        @Override
        public void delete(DeleteObjectCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            throw new UnsupportedOperationException();
        }
    }
}
