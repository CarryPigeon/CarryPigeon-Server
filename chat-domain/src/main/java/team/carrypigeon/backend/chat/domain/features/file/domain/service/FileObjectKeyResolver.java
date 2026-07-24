package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
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

    private final ChannelMessagingApi channelMessagingApi;
    private final FileUploadShareKeyCodec uploadShareKeyCodec;

    FileObjectKeyResolver(
            ChannelMessagingApi channelMessagingApi,
            FileUploadShareKeyCodec uploadShareKeyCodec
    ) {
        this.channelMessagingApi = channelMessagingApi;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
    }

    boolean isServerAvatar(String shareKey) {
        return SERVER_AVATAR_SHARE_KEY.equals(shareKey);
    }

    /**
     * 生成账号个人背景图的稳定 share_key。
     * 输入：背景图所属账号 ID。
     * 输出：仅该账号可用于上传或下载解析的个人背景图 share_key。
     * 失败语义：账号 ID 非正数时抛出参数校验问题。
     *
     * @param accountId 背景图所属账号 ID
     * @return 个人背景图 share_key
     */
    String profileBackgroundShareKey(long accountId) {
        if (accountId <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        return PROFILE_BACKGROUND_SHARE_KEY_PREFIX + accountId;
    }

    /**
     * 把对外 share_key 解析为可读取的对象存储 key。
     * 输入：当前登录账号 ID 和客户端提交的 share_key。
     * 输出：完成访问约束校验后的 objectKey。
     * 失败语义：未登录、share_key 非法或无权访问附件/个人背景图时抛出领域问题异常。
     *
     * @param accountId 当前登录账号 ID，公开服务端头像之外的文件必须提供
     * @param shareKey 客户端提交的文件引用 share_key
     * @return 可交给对象存储读取的 objectKey
     */
    String resolveDownloadObjectKey(Long accountId, String shareKey) {
        if (shareKey == null || shareKey.isBlank()) {
            throw ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
        }
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

    /**
     * 把上传 share_key 解析为允许写入的对象存储目标。
     * 输入：上传账号、上传授权 share_key 和实际内容长度。
     * 输出：对象存储 key 与允许写入的对象大小。
     * 失败语义：只读附件引用、服务端头像、归属账号不匹配、大小不匹配或 share_key 非法时抛出领域问题异常。
     *
     * @param accountId 发起上传的账号 ID
     * @param shareKey 客户端提交的上传 share_key
     * @param sizeBytes 实际上传内容长度
     * @return 已校验的对象存储写入目标
     */
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

    /**
     * 校验当前账号是否可下载频道消息附件。
     * 输入：当前账号 ID 与已从 share_key 解析出的附件 objectKey。
     * 失败语义：objectKey 结构非法或账号无频道访问权时抛出领域问题异常。
     *
     * @param accountId 当前账号 ID
     * @param objectKey 附件对象存储 key
     */
    private void authorizeAttachmentDownload(long accountId, String objectKey) {
        AttachmentScope attachmentScope = parseAttachmentScope(objectKey);
        if (!channelMessagingApi.isMember(attachmentScope.channelId(), accountId)) {
            throw ProblemException.forbidden("file_access_forbidden", "file access is not granted to current account");
        }
    }

    /**
     * 从附件 objectKey 中解析频道、消息类型和发送者范围。
     * 约束：只接受消息附件约定路径，避免非附件对象绕过下载权限校验。
     *
     * @param objectKey 附件对象存储 key
     * @return 附件所属范围
     */
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

    /**
     * 从个人背景图 share_key 中解析归属账号。
     * 失败语义：后缀不是合法账号 ID 时按非法 share_key 处理。
     *
     * @param shareKey 个人背景图 share_key
     * @return 背景图归属账号 ID
     */
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

    /**
     * 要求当前文件访问具备已认证账号。
     * 语义：除公开服务端头像外，所有文件下载都必须绑定账号权限。
     *
     * @param accountId 当前账号 ID
     * @return 非空且为正数的账号 ID
     */
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
