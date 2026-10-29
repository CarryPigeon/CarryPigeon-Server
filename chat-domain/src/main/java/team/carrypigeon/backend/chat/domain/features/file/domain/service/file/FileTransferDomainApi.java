package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 文件领域服务。
 * 职责：为上传申请、上传写入和下载访问提供最小编排能力。
 * 边界：不引入独立文件表，只依赖对象存储能力与稳定 share_key 规则。
 */
@Service
public class FileTransferDomainApi implements FileTransferApi {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(30);

    private final ObjectStorageService objectStorageService;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final FileUploadShareKeyCodec uploadShareKeyCodec;

    public FileTransferDomainApi(
            ObjectStorageService objectStorageService,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            FileUploadShareKeyCodec uploadShareKeyCodec
    ) {
        this.objectStorageService = objectStorageService;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
    }

    @Override
    public FileUploadGrantResult createUploadGrant(long accountId, String filename, String mimeType, long sizeBytes) {
        long fileId = idGenerator.nextLongId();
        Instant expiresAt = timeProvider.nowInstant().plus(UPLOAD_URL_TTL);
        String shareKey = uploadShareKeyCodec.issue(accountId, fileId, sizeBytes, expiresAt);
        return new FileUploadGrantResult(
                fileId,
                shareKey,
                "/api/files/uploads/" + shareKey,
                expiresAt
        );
    }

    @Override
    public void uploadFile(long accountId, String shareKey, String contentType, long sizeBytes, InputStream content) {
        // 进行相关校验
        FileUploadShareKeyCodec.IssuedUploadShareKey parse = uploadShareKeyCodec.parse(shareKey, timeProvider.nowInstant());
        if (parse.ownerAccountId() != accountId) {
            throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
        }
        if (parse.expiresAtEpochMillis() < timeProvider.nowInstant().toEpochMilli()) {
            throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
        }
        if (parse.declaredSizeBytes() != sizeBytes) {
            throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
        }

        objectStorageService.put("/files/" + shareKey, content, sizeBytes, contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim());
    }


    @Override
    public Optional<FileDownloadResult> downloadFile(String shareKey) {
        String objectKey = "/files/" + shareKey;
        Optional<StorageObject> storageObject = objectStorageService.get(objectKey);
        if (storageObject.isEmpty()) {
            return Optional.empty();
        }
        StorageObject object = storageObject.get();
        Optional<URI> redirectUrl = object.content().isPresent()
                ? Optional.empty()
                : Optional.of(objectStorageService.createPresignedUrl(objectKey, DOWNLOAD_URL_TTL).url());
        return Optional.of(new FileDownloadResult(
                object.contentType(),
                object.size(),
                object.content(),
                redirectUrl
        ));
    }
}
