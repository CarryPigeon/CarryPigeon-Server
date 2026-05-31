package team.carrypigeon.backend.chat.domain.features.file.support;

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

    public static String shareKeyForObjectKey(String objectKey) {
        String normalizedObjectKey = requireNonBlank(objectKey, "objectKey");
        if (normalizedObjectKey.startsWith(FILE_OBJECT_KEY_PREFIX)) {
            return normalizedObjectKey.substring(FILE_OBJECT_KEY_PREFIX.length());
        }
        return ATTACHMENT_SHARE_KEY_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalizedObjectKey.getBytes(StandardCharsets.UTF_8));
    }

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

    public static String uploadObjectKey(String shareKey) {
        return FILE_OBJECT_KEY_PREFIX + requireNonBlank(shareKey, "shareKey");
    }

    public static String resolveObjectKey(String shareKey) {
        return attachmentObjectKey(shareKey).orElseGet(() -> uploadObjectKey(shareKey));
    }

    public static String downloadPath(String shareKey) {
        return "api/files/download/" + requireNonBlank(shareKey, "shareKey");
    }

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
