package team.carrypigeon.backend.chat.domain.features.file.application.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.file.application.dto.FileUploadGrantResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("contract")
class FileApplicationServiceTests {

    @Test
    @DisplayName("create upload grant returns stable share key and same-origin upload path")
    void createUploadGrant_returnsStableShareKeyAndUploadPath() {
        FileApplicationService service = createService(new RecordingObjectStorageService());

        FileUploadGrantResult result = service.createUploadGrant("image.png", "image/png", 123L);

        assertEquals(7001L, result.fileId());
        assertEquals("shr_7001", result.shareKey());
        assertEquals("/api/files/uploads/shr_7001", result.uploadUrl());
    }

    @Test
    @DisplayName("download url uses derived file object key")
    void createDownloadUrl_usesDerivedFileObjectKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileApplicationService service = createService(storageService);

        service.createDownloadUrl("shr_7001");

        assertEquals("files/shr_7001", storageService.lastPresignedObjectKey);
    }

    @Test
    @DisplayName("server avatar keeps reserved object key")
    void serverAvatar_keepsReservedObjectKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        storageService.object = StorageObject.metadata("server_avatar", "image/png", 12L);
        FileApplicationService service = createService(storageService);

        Optional<StorageObject> result = service.findStorageObject("server_avatar");

        assertTrue(result.isPresent());
        assertEquals("server_avatar", storageService.lastGetObjectKey);
    }

    private FileApplicationService createService(RecordingObjectStorageService storageService) {
        return new FileApplicationService(new StaticObjectProvider(storageService), new FixedIdGenerator(), new TimeProvider(Clock.fixed(Instant.parse("2026-04-23T00:00:00Z"), ZoneOffset.UTC)));
    }

    private static final class FixedIdGenerator implements IdGenerator {
        @Override
        public long nextLongId() {
            return 7001L;
        }
    }

    private static final class StaticObjectProvider implements ObjectProvider<ObjectStorageService> {
        private final ObjectStorageService storageService;

        private StaticObjectProvider(ObjectStorageService storageService) {
            this.storageService = storageService;
        }

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
    }

    private static final class RecordingObjectStorageService implements ObjectStorageService {
        private String lastPresignedObjectKey;
        private String lastGetObjectKey;
        private StorageObject object;

        @Override
        public StorageObject put(PutObjectCommand command) {
            return StorageObject.metadata(command.objectKey(), command.contentType(), command.size());
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            lastGetObjectKey = command.objectKey();
            return Optional.ofNullable(object);
        }

        @Override
        public void delete(DeleteObjectCommand command) {
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            lastPresignedObjectKey = command.objectKey();
            return new PresignedUrl(URI.create("http://test.local/objects/" + command.objectKey()), Instant.parse("2026-04-23T01:00:00Z"));
        }
    }
}
