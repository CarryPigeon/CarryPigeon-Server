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
     * 验证 file payload 会附加临时访问 URL。
     */
    @Test
    @DisplayName("resolve file payload adds access url fields")
    void resolve_filePayload_addsAccessUrlFields() {
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                Instant.parse("2026-04-22T00:30:00Z")
        );
        JsonProvider jsonProvider = jsonProvider();
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(objectProvider(storageService), jsonProvider);
        String payload = jsonProvider.toJson(Map.of(
                "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                "filename", "demo.pdf",
                "mime_type", "application/pdf",
                "size", 1024L
        ));

        String resolved = resolver.resolve("file", payload);

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", jsonProvider.readTree(resolved).path("object_key").asText());
        assertEquals("http://127.0.0.1:9000/file/demo.pdf?token=abc", jsonProvider.readTree(resolved).path("access_url").asText());
        assertEquals(
                Instant.parse("2026-04-22T00:30:00Z").getEpochSecond(),
                jsonProvider.readTree(resolved).path("access_url_expires_at").asLong()
        );
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
     * 验证派生 URL 失败时会回退原始 payload。
     */
    @Test
    @DisplayName("resolve presign failure returns original payload")
    void resolve_presignFailure_returnsOriginalPayload() {
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.failOnPresign = true;
        JsonProvider jsonProvider = jsonProvider();
        RecordingMessageAttachmentPayloadResolver resolver = new RecordingMessageAttachmentPayloadResolver(
                objectProvider(storageService),
                jsonProvider
        );
        String payload = jsonProvider.toJson(Map.of(
                "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                "filename", "demo.pdf"
        ));

        String resolved = resolver.resolve("file", payload);

        assertEquals(payload, resolved);
        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", resolver.loggedObjectKey);
        assertEquals("boom", resolver.loggedException.getMessage());
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

    private static class TestObjectStorageService implements ObjectStorageService {

        private PresignedUrl presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/default"),
                Instant.parse("2026-04-22T00:30:00Z")
        );
        private boolean failOnPresign;

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
            if (failOnPresign) {
                throw new IllegalStateException("boom");
            }
            return presignedUrl;
        }
    }

    private static class RecordingMessageAttachmentPayloadResolver extends MessageAttachmentPayloadResolver {

        private String loggedObjectKey;
        private RuntimeException loggedException;

        private RecordingMessageAttachmentPayloadResolver(
                ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
                JsonProvider jsonProvider
        ) {
            super(objectStorageServiceProvider, jsonProvider);
        }

        @Override
        void logPresignFallback(String objectKey, RuntimeException exception) {
            this.loggedObjectKey = objectKey;
            this.loggedException = exception;
        }
    }
}
