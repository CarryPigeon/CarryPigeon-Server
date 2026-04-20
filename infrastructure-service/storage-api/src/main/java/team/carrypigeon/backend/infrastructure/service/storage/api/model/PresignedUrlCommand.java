package team.carrypigeon.backend.infrastructure.service.storage.api.model;

import java.time.Duration;

/**
 * 预签名 URL 生成命令。
 * 职责：承载对象键和 URL 有效期。
 * 边界：不暴露具体对象存储 SDK 的请求对象。
 *
 * @param objectKey 对象键
 * @param ttl URL 有效期
 */
public record PresignedUrlCommand(String objectKey, Duration ttl) {

    public PresignedUrlCommand {
        PutObjectCommand.validateObjectKey(objectKey);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("presigned url ttl must be positive");
        }
    }
}
