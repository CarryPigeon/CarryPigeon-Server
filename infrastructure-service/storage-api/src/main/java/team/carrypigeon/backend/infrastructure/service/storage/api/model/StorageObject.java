package team.carrypigeon.backend.infrastructure.service.storage.api.model;

import java.io.InputStream;
import java.util.Optional;

/**
 * 对象存储条目。
 * 职责：表达对象键、元信息和可选内容入口。
 * 边界：不包含 MinIO bucket、etag 规则或 SDK 专有模型。
 *
 * @param objectKey 对象键
 * @param contentType 内容类型
 * @param size 对象字节数
 * @param content 对象内容流；仅元信息场景可为空
 */
public record StorageObject(String objectKey, String contentType, long size, Optional<InputStream> content) {

    public static StorageObject metadata(String objectKey, String contentType, long size) {
        return new StorageObject(objectKey, contentType, size, Optional.empty());
    }
}
