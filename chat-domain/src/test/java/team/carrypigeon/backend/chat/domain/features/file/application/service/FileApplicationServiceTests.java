package team.carrypigeon.backend.chat.domain.features.file.application.service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.file.application.dto.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.support.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("contract")
class FileApplicationServiceTests {

    @Test
    @DisplayName("create upload grant returns stable share key and same-origin upload path")
    void createUploadGrant_returnsStableShareKeyAndUploadPath() {
        FileApplicationService service = createService(new RecordingObjectStorageService());

        FileUploadGrantResult result = service.createUploadGrant(1001L, "image.png", "image/png", 123L);

        assertEquals(7001L, result.fileId());
        assertTrue(result.shareKey().startsWith("shr_"));
        assertEquals("/api/files/uploads/" + result.shareKey(), result.uploadUrl());
    }

    @Test
    @DisplayName("download url uses derived file object key")
    void createDownloadUrl_usesDerivedFileObjectKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileApplicationService service = createService(storageService);
        String shareKey = service.createUploadGrant(1001L, "image.png", "image/png", 123L).shareKey();

        service.createDownloadUrl(1001L, shareKey);

        assertEquals("files/accounts/1001/7001", storageService.lastPresignedObjectKey);
    }

    @Test
    @DisplayName("server avatar keeps reserved object key")
    void serverAvatar_keepsReservedObjectKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        storageService.object = StorageObject.metadata("server_avatar", "image/png", 12L);
        FileApplicationService service = createService(storageService);

        Optional<StorageObject> result = service.findStorageObject(null, "server_avatar");

        assertTrue(result.isPresent());
        assertEquals("server_avatar", storageService.lastGetObjectKey);
    }

    @Test
    @DisplayName("find storage object foreign upload share key throws forbidden problem")
    void findStorageObject_foreignUploadShareKey_throwsForbiddenProblem() {
        FileApplicationService service = createService(new RecordingObjectStorageService());
        String shareKey = service.createUploadGrant(1001L, "image.png", "image/png", 123L).shareKey();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.findStorageObject(1002L, shareKey)
        );

        assertEquals("file access is not granted to current account", exception.getMessage());
    }

    @Test
    @DisplayName("find storage object attachment without membership throws forbidden problem")
    void findStorageObject_attachmentWithoutMembership_throwsForbiddenProblem() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileApplicationService service = createService(storageService);
        String shareKey = FileShareKeyCodec.shareKeyForObjectKey("channels/9/messages/file/accounts/1001/5001/demo.png");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.findStorageObject(1002L, shareKey)
        );

        assertEquals("file access is not granted to current account", exception.getMessage());
    }

    private FileApplicationService createService(RecordingObjectStorageService storageService) {
        return new FileApplicationService(
                new StaticObjectProvider(storageService),
                new NoopChannelMemberRepository(),
                new FixedIdGenerator(),
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-23T00:00:00Z"), ZoneOffset.UTC)),
                new AuthJwtProperties("carrypigeon", "0123456789abcdef0123456789abcdef", Duration.ofMinutes(30), Duration.ofDays(14))
        );
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

    private static final class NoopChannelMemberRepository implements ChannelMemberRepository {
        @Override
        public boolean exists(long channelId, long accountId) {
            return false;
        }

        @Override
        public void save(ChannelMember channelMember) {
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return List.of();
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
