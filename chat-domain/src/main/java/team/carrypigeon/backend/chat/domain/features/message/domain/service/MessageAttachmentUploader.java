package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.io.InputStream;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息附件上传协作对象。
 * 职责：封装附件类型规范化、对象 key 生成、对象存储写入和上传结果组装。
 * 边界：不读取频道仓储、不校验成员权限、不创建消息记录。
 */
class MessageAttachmentUploader {

    private static final String FILE_MESSAGE_TYPE = "file";
    private static final String VOICE_MESSAGE_TYPE = "voice";

    private final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;
    private final IdGenerator idGenerator;
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    MessageAttachmentUploader(
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            IdGenerator idGenerator,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
        this.idGenerator = idGenerator;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    MessageAttachmentUploadResult upload(
            long accountId,
            long channelId,
            String messageType,
            String filename,
            String contentType,
            long size,
            InputStream content
    ) {
        if (content == null) {
            throw ProblemException.validationFailed("file content must not be null");
        }
        if (size <= 0L) {
            throw ProblemException.validationFailed("size must be greater than 0");
        }
        String normalizedMessageType = normalizeAttachmentMessageType(messageType);
        String normalizedFilename = messageAttachmentObjectKeyPolicy.normalizeFilename(filename);
        String resolvedContentType = resolveContentType(normalizedMessageType, contentType);
        String objectKey = messageAttachmentObjectKeyPolicy.buildObjectKey(
                channelId,
                normalizedMessageType,
                accountId,
                idGenerator.nextLongId(),
                normalizedFilename
        );
        requireObjectStorageService().put(new PutObjectCommand(objectKey, content, size, resolvedContentType));
        return new MessageAttachmentUploadResult(
                objectKey,
                FileShareKeyCodec.shareKeyForObjectKey(objectKey),
                normalizedFilename,
                resolvedContentType,
                size
        );
    }

    private String normalizeAttachmentMessageType(String messageType) {
        String normalized = messageType == null || messageType.isBlank() ? FILE_MESSAGE_TYPE : messageType.trim().toLowerCase();
        if (!FILE_MESSAGE_TYPE.equals(normalized) && !VOICE_MESSAGE_TYPE.equals(normalized)) {
            throw ProblemException.validationFailed("message_type must be file or voice");
        }
        return normalized;
    }

    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    private String resolveContentType(String messageType, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        return VOICE_MESSAGE_TYPE.equals(messageType) ? "audio/mpeg" : "application/octet-stream";
    }
}
