package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * 文件 share_key 编解码器。
 * 职责：统一对外 `share_key` 与内部对象存储键之间的转换规则。
 * 边界：只处理文件协议中的稳定标识映射，不承载对象存储访问。
 */
public final class FileShareKeyCodec {

    private static final String FILE_OBJECT_KEY_PREFIX = "files/";
    private static final String ATTACHMENT_SHARE_KEY_PREFIX = "shr_att_";

    private FileShareKeyCodec() {
    }

    /**
     * 从内部对象键生成对外 `share_key`。
     * 约束：常规上传对象保留 `files/` 后缀，其它附件对象键会被编码为不透明 share key。
     */
    public static String shareKeyForObjectKey(String objectKey) {
        String normalizedObjectKey = requireNonBlank(objectKey, "objectKey");
        if (normalizedObjectKey.startsWith(FILE_OBJECT_KEY_PREFIX)) {
            return normalizedObjectKey.substring(FILE_OBJECT_KEY_PREFIX.length());
        }
        return ATTACHMENT_SHARE_KEY_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalizedObjectKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从对外 `share_key` 反解附件对象键。
     * 输出：仅当 share key 属于附件编码格式时返回对象键。
     */
    public static Optional<String> attachmentObjectKey(String shareKey) {
        String normalizedShareKey = requireNonBlank(shareKey, "shareKey");
        if (!normalizedShareKey.startsWith(ATTACHMENT_SHARE_KEY_PREFIX)) {
            return Optional.empty();
        }
        String encoded = normalizedShareKey.substring(ATTACHMENT_SHARE_KEY_PREFIX.length());
        try {
            return Optional.of(new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * 为常规上传文件构造 canonical object key。
     */
    public static String uploadObjectKey(String shareKey) {
        return FILE_OBJECT_KEY_PREFIX + requireNonBlank(shareKey, "shareKey");
    }

    /**
     * 解析 `share_key` 对应的最终对象键。
     * 原因：统一兼容普通上传对象和附件编码对象两类来源。
     */
    public static String resolveObjectKey(String shareKey) {
        return attachmentObjectKey(shareKey).orElseGet(() -> uploadObjectKey(shareKey));
    }

    /**
     * 生成对外下载路径。
     */
    public static String downloadPath(String shareKey) {
        return "/api/files/download/" + requireNonBlank(shareKey, "shareKey");
    }

    /**
     * 判断对象键是否属于常规上传文件命名空间。
     */
    public static boolean isUploadObjectKey(String objectKey) {
        return objectKey != null && objectKey.startsWith(FILE_OBJECT_KEY_PREFIX);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
