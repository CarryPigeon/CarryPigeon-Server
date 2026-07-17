package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.net.URI;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileUploadShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileShareKeyCodec;
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
/**
 * `FileTransferDomainApi` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class FileTransferDomainApiTests {

    /**
     * 验证 `createUploadGrant` 在 `returnsStableShareKeyAndUploadPath` 场景下的测试契约。
     */
    @Test
    @DisplayName("create upload grant returns stable share key and same-origin upload path")
    void createUploadGrant_returnsStableShareKeyAndUploadPath() {
        FileTransferDomainApi service = createService(new RecordingObjectStorageService());

        FileUploadGrantResult result = service.createUploadGrant(1001L, "image.png", "image/png", 123L);

        assertEquals(7001L, result.fileId());
        assertTrue(result.shareKey().startsWith("shr_"));
        assertEquals("/api/files/uploads/" + result.shareKey(), result.uploadUrl());
    }

    /**
     * 验证 `createUploadGrant` 在账号 ID 非法时返回业务校验异常。
     */
    @Test
    @DisplayName("create upload grant invalid account id throws validation problem")
    void createUploadGrant_invalidAccountId_throwsValidationProblem() {
        FileTransferDomainApi service = createService(new RecordingObjectStorageService());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.createUploadGrant(0L, "image.png", "image/png", 123L)
        );

        assertEquals("accountId must be greater than 0", exception.getMessage());
    }

    /**
     * 验证 `downloadFile` 在 `withoutContent` 条件下满足 `usesDerivedFileObjectKeyForRedirect` 的测试契约。
     */
    @Test
    @DisplayName("download file without content uses derived file object key for redirect")
    void downloadFile_withoutContent_usesDerivedFileObjectKeyForRedirect() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileTransferDomainApi service = createService(storageService);
        String shareKey = service.createUploadGrant(1001L, "image.png", "image/png", 123L).shareKey();
        storageService.object = StorageObject.metadata("files/accounts/1001/7001", "image/png", 123L);

        Optional<FileDownloadResult> result = service.downloadFile(1001L, shareKey);

        assertTrue(result.isPresent());
        assertTrue(result.get().redirectUrl().isPresent());
        assertEquals("files/accounts/1001/7001", storageService.lastPresignedObjectKey);
    }

    /**
     * 验证 `serverAvatar` 在 `keepsReservedObjectKey` 场景下的测试契约。
     */
    @Test
    @DisplayName("server avatar keeps reserved object key")
    void serverAvatar_keepsReservedObjectKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        storageService.object = StorageObject.metadata("server_avatar", "image/png", 12L);
        FileTransferDomainApi service = createService(storageService);

        Optional<FileDownloadResult> result = service.downloadFile(null, "server_avatar");

        assertTrue(result.isPresent());
        assertEquals("server_avatar", storageService.lastGetObjectKey);
    }

    /**
     * 验证 `downloadFile` 在 `foreignUploadShareKey` 条件下满足 `throwsForbiddenProblem` 的测试契约。
     */
    @Test
    @DisplayName("download file foreign upload share key throws forbidden problem")
    void downloadFile_foreignUploadShareKey_throwsForbiddenProblem() {
        FileTransferDomainApi service = createService(new RecordingObjectStorageService());
        String shareKey = service.createUploadGrant(1001L, "image.png", "image/png", 123L).shareKey();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.downloadFile(1002L, shareKey)
        );

        assertEquals("file access is not granted to current account", exception.getMessage());
    }

    /**
     * 验证 `downloadFile` 在 `attachmentWithoutMembership` 条件下满足 `throwsForbiddenProblem` 的测试契约。
     */
    @Test
    @DisplayName("download file attachment without membership throws forbidden problem")
    void downloadFile_attachmentWithoutMembership_throwsForbiddenProblem() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileTransferDomainApi service = createService(storageService);
        String shareKey = FileShareKeyCodec.shareKeyForObjectKey("channels/9/messages/file/accounts/1001/5001/demo.png");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.downloadFile(1002L, shareKey)
        );

        assertEquals("file access is not granted to current account", exception.getMessage());
    }

    /**
     * 验证空白下载 share_key 会收口为领域校验问题，而不是泄漏为未分类运行时异常。
     */
    @Test
    @DisplayName("download file blank share key throws validation problem")
    void downloadFile_blankShareKey_throwsValidationProblem() {
        FileTransferDomainApi service = createService(new RecordingObjectStorageService());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.downloadFile(1001L, " ")
        );

        assertEquals("invalid_share_key", exception.reason());
        assertEquals("share_key is invalid", exception.getMessage());
    }

    /**
     * 验证上传写入仍使用授权 share_key 派生的对象 key，并规范化空 content-type。
     */
    @Test
    @DisplayName("upload file uses issued object key and default content type")
    void uploadFile_issuedShareKey_usesObjectKeyAndDefaultContentType() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileTransferDomainApi service = createService(storageService);
        String shareKey = service.createUploadGrant(1001L, "image.png", "image/png", 123L).shareKey();

        service.uploadFile(1001L, shareKey, " ", 123L, new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertEquals("files/accounts/1001/7001", storageService.lastPutObjectKey);
        assertEquals("application/octet-stream", storageService.lastPutContentType);
    }

    /**
     * 验证资料背景图上传由文件领域生成固定 share key 并写入固定对象位置。
     */
    @Test
    @DisplayName("upload profile background returns domain share key")
    void uploadProfileBackground_returnsDomainShareKey() {
        RecordingObjectStorageService storageService = new RecordingObjectStorageService();
        FileTransferDomainApi service = createService(storageService);

        String shareKey = service.uploadProfileBackground(1001L, "image/png", 123L, new ByteArrayInputStream(new byte[] {1}));

        assertEquals("profile_bg_1001", shareKey);
        assertEquals("files/profile-background/1001", storageService.lastPutObjectKey);
        assertEquals("image/png", storageService.lastPutContentType);
    }

    private FileTransferDomainApi createService(RecordingObjectStorageService storageService) {
        return new FileTransferDomainApi(
                new StaticObjectProvider(storageService),
                (accountId, channelId) -> false,
                new FixedIdGenerator(),
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-23T00:00:00Z"), ZoneOffset.UTC)),
                new FileUploadShareKeyCodec("0123456789abcdef0123456789abcdef")
        );
    }

    /**
     * `FixedIdGenerator` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class FixedIdGenerator implements IdGenerator {
        @Override
        public long nextLongId() {
            return 7001L;
        }
    }

    /**
     * `StaticObjectProvider` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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

    /**
     * `RecordingObjectStorageService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingObjectStorageService implements ObjectStorageService {
        private String lastPresignedObjectKey;
        private String lastGetObjectKey;
        private String lastPutObjectKey;
        private String lastPutContentType;
        private StorageObject object;

        @Override
        public StorageObject put(PutObjectCommand command) {
            lastPutObjectKey = command.objectKey();
            lastPutContentType = command.contentType();
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
