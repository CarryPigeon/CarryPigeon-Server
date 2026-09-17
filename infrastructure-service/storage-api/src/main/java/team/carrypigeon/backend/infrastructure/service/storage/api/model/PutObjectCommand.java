package team.carrypigeon.backend.infrastructure.service.storage.api.model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 上传对象命令。
 * 职责：承载对象写入所需的业务无关参数。
 * 边界：不包含 MinIO bucket 或 SDK 专有参数。
 *
 * @param objectKey 对象键
 * @param content 对象内容流，由调用方负责关闭
 * @param size 对象字节数
 * @param contentType 内容类型
 */
public record PutObjectCommand(String objectKey, InputStream content, long size, String contentType) {

    private static final int MAX_OBJECT_KEY_BYTES = 1024;

    public PutObjectCommand {
        validateObjectKey(objectKey);
        if (content == null) {
            throw new IllegalArgumentException("storage object content must not be null");
        }
        if (size < 0) {
            throw new IllegalArgumentException("storage object size must not be negative");
        }
    }

    static void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("storage object key must not be blank");
        }
        if (objectKey.getBytes(StandardCharsets.UTF_8).length > MAX_OBJECT_KEY_BYTES) {
            throw new IllegalArgumentException("storage object key must not exceed 1024 UTF-8 bytes");
        }
        if (objectKey.startsWith("/") || objectKey.startsWith("\\")) {
            throw new IllegalArgumentException("storage object key must be relative");
        }
        if (objectKey.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("storage object key must not contain control characters");
        }
        for (String segment : objectKey.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("storage object key contains an invalid path segment");
            }
        }
    }
}
