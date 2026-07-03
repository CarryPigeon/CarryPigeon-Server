package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.file.domain.port.FileAttachmentAccessAuthorizer;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 文件对象 key 解析协作对象。
 * 职责：维护 share_key、特殊文件 key 与对象存储 objectKey 的转换和访问约束。
 * 边界：不调用对象存储、不生成上传授权、不返回 HTTP 协议对象。
 */
class FileObjectKeyResolver {

    private static final String SERVER_AVATAR_SHARE_KEY = "server_avatar";
    private static final String PROFILE_BACKGROUND_SHARE_KEY_PREFIX = "profile_bg_";
    private static final long MAX_PROFILE_BACKGROUND_SIZE_BYTES = 10L * 1024 * 1024;

    private final FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer;
    private final FileUploadShareKeyCodec uploadShareKeyCodec;

    FileObjectKeyResolver(
            FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer,
            FileUploadShareKeyCodec uploadShareKeyCodec
    ) {
        this.fileAttachmentAccessAuthorizer = fileAttachmentAccessAuthorizer;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
    }

    boolean isServerAvatar(String shareKey) {
        return SERVER_AVATAR_SHARE_KEY.equals(shareKey);
    }

    String resolveDownloadObjectKey(Long accountId, String shareKey) {
        if (SERVER_AVATAR_SHARE_KEY.equals(shareKey)) {
            return SERVER_AVATAR_SHARE_KEY;
        }
        long requiredAccountId = requireAuthenticatedAccountId(accountId);
        if (isProfileBackgroundShareKey(shareKey)) {
            long ownerAccountId = parseProfileBackgroundAccountId(shareKey);
            if (ownerAccountId != requiredAccountId) {
                throw ProblemException.forbidden("file_access_forbidden", "file access is not granted to current account");
            }
            return profileBackgroundObjectKey(ownerAccountId);
        }
        Optional<String> attachmentObjectKey = FileShareKeyCodec.attachmentObjectKey(shareKey);
        if (attachmentObjectKey.isPresent()) {
            authorizeAttachmentDownload(requiredAccountId, attachmentObjectKey.get());
            return attachmentObjectKey.get();
        }
        FileUploadShareKeyCodec.IssuedUploadShareKey issuedShareKey = uploadShareKeyCodec.parse(shareKey);
        if (issuedShareKey.ownerAccountId() != requiredAccountId) {
            throw ProblemException.forbidden("file_access_forbidden", "file access is not granted to current account");
        }
        return uploadShareKeyCodec.objectKey(issuedShareKey);
    }

    ResolvedObject resolveUploadObject(long accountId, String shareKey, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw ProblemException.validationFailed("size_bytes must be greater than 0");
        }
        if (FileShareKeyCodec.attachmentObjectKey(shareKey).isPresent()) {
            throw ProblemException.validationFailed("attachment share_key is read-only");
        }
        if (SERVER_AVATAR_SHARE_KEY.equals(shareKey)) {
            throw ProblemException.forbidden("file_upload_forbidden", "server avatar upload is not supported");
        }
        if (isProfileBackgroundShareKey(shareKey)) {
            long ownerAccountId = parseProfileBackgroundAccountId(shareKey);
            if (ownerAccountId != accountId) {
                throw ProblemException.forbidden("file_upload_forbidden", "file upload is not granted to current account");
            }
            if (sizeBytes > MAX_PROFILE_BACKGROUND_SIZE_BYTES) {
                throw ProblemException.validationFailed(
                        "size_bytes must be less than or equal to " + MAX_PROFILE_BACKGROUND_SIZE_BYTES
                );
            }
            return new ResolvedObject(profileBackgroundObjectKey(ownerAccountId), sizeBytes);
        }
        if (!shareKey.startsWith("shr_")) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
        FileUploadShareKeyCodec.IssuedUploadShareKey issuedShareKey = uploadShareKeyCodec.parse(shareKey);
        if (issuedShareKey.ownerAccountId() != accountId) {
            throw ProblemException.forbidden("file_upload_forbidden", "file upload is not granted to current account");
        }
        if (issuedShareKey.declaredSizeBytes() != sizeBytes) {
            throw ProblemException.validationFailed("declared size does not match upload content length");
        }
        return new ResolvedObject(uploadShareKeyCodec.objectKey(issuedShareKey), sizeBytes);
    }

    private void authorizeAttachmentDownload(long accountId, String objectKey) {
        AttachmentScope attachmentScope = parseAttachmentScope(objectKey);
        if (!fileAttachmentAccessAuthorizer.canAccessChannelAttachment(accountId, attachmentScope.channelId())) {
            throw ProblemException.forbidden("file_access_forbidden", "file access is not granted to current account");
        }
    }

    private AttachmentScope parseAttachmentScope(String objectKey) {
        String[] segments = objectKey.split("/");
        if (segments.length < 7) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
        if (!"channels".equals(segments[0]) || !"messages".equals(segments[2]) || !"accounts".equals(segments[4])) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
        try {
            return new AttachmentScope(Long.parseLong(segments[1]), segments[3], Long.parseLong(segments[5]));
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
    }

    private boolean isProfileBackgroundShareKey(String shareKey) {
        return shareKey != null && shareKey.startsWith(PROFILE_BACKGROUND_SHARE_KEY_PREFIX);
    }

    private long parseProfileBackgroundAccountId(String shareKey) {
        try {
            return Long.parseLong(shareKey.substring(PROFILE_BACKGROUND_SHARE_KEY_PREFIX.length()));
        } catch (RuntimeException exception) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
    }

    private String profileBackgroundObjectKey(long accountId) {
        return "files/profile-background/" + accountId;
    }

    private long requireAuthenticatedAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw ProblemException.forbidden("authentication_required", "authentication is required");
        }
        return accountId;
    }

    /**
     * 已解析出的对象存储引用。
     * 职责：在文件下载流程中承载对象键和对象大小。
     */
    record ResolvedObject(String objectKey, long sizeBytes) {
    }

    /**
     * 附件对象键解析出的归属范围。
     * 职责：用于校验下载者是否有权访问指定频道、消息类型和发送者范围内的附件。
     *
     * @param channelId 附件所属频道 ID
     * @param messageType 附件所属消息类型
     * @param senderAccountId 附件上传者账号 ID
     */
    private record AttachmentScope(long channelId, String messageType, long senderAccountId) {
    }
}
