package team.carrypigeon.backend.chat.domain.features.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件 share key 配置。
 * 职责：承载 file feature 自有的上传 share key 签名密钥。
 * 边界：不复用 auth token 配置，不承载文件访问业务规则。
 *
 * @param secret 上传 share key 签名密钥
 */
@ConfigurationProperties(prefix = "cp.chat.file.share-key")
public record FileShareKeyProperties(String secret) {

    private static final int MIN_SECRET_LENGTH = 32;

    public FileShareKeyProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("cp.chat.file.share-key.secret must not be blank");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("cp.chat.file.share-key.secret must be at least 32 characters");
        }
    }
}
