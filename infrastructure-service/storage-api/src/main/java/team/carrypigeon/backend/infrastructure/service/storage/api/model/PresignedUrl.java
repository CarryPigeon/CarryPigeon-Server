package team.carrypigeon.backend.infrastructure.service.storage.api.model;

import java.net.URI;
import java.time.Instant;

/**
 * 预签名 URL 结果。
 * 职责：表达可临时访问对象的 URL 和过期时间。
 *
 * @param url 临时访问 URL
 * @param expiresAt URL 过期时间
 */
public record PresignedUrl(URI url, Instant expiresAt) {

    public PresignedUrl {
        if (url == null) {
            throw new IllegalArgumentException("presigned url must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("presigned url expiresAt must not be null");
        }
    }
}
