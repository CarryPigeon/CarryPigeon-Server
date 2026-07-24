package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;

/**
 * 文件引用 API 实现。
 * 职责：在 file feature 内统一 share key 与对象键的稳定转换规则。
 * 边界：不访问对象存储，不执行文件下载权限校验。
 */
@Service
public class FileReferenceDomainApi implements FileReferenceApi {

    private final FileMessageAttachmentPolicy messageAttachmentPolicy = new FileMessageAttachmentPolicy();

    @Override
    public String shareKeyForObjectKey(String objectKey) {
        return FileShareKeyCodec.shareKeyForObjectKey(objectKey);
    }

    @Override
    public Optional<String> attachmentObjectKey(String shareKey) {
        return FileShareKeyCodec.attachmentObjectKey(shareKey);
    }

    @Override
    public String downloadPath(String shareKey) {
        return FileShareKeyCodec.downloadPath(shareKey);
    }

    @Override
    public String normalizeMessageAttachmentFilename(String filename) {
        return messageAttachmentPolicy.normalizeFilename(filename);
    }

    @Override
    public String buildMessageAttachmentObjectKey(
            long channelId,
            String messageType,
            long accountId,
            long objectId,
            String filename
    ) {
        return messageAttachmentPolicy.buildObjectKey(channelId, messageType, accountId, objectId, filename);
    }

    @Override
    public boolean isMessageAttachmentWithinSenderScope(
            long channelId,
            String messageType,
            long accountId,
            String objectKey
    ) {
        return messageAttachmentPolicy.isWithinSenderScope(channelId, messageType, accountId, objectKey);
    }
}
