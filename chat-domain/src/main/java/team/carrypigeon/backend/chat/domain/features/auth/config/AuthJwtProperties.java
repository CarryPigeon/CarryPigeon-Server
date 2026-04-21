package team.carrypigeon.backend.chat.domain.features.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权 JWT 配置。
 * 职责：承载 access token 与 refresh token 的最小签发配置。
 * 边界：只表达运行时差异，不承载令牌签发业务流程。
 *
 * @param issuer 当前服务端签发者标识
 * @param secret HS256 签名密钥
 * @param accessTokenTtl access token 有效期
 * @param refreshTokenTtl refresh token 有效期
 */
@ConfigurationProperties(prefix = "cp.chat.auth.jwt")
public record AuthJwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {

    public AuthJwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "carrypigeon";
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("cp.chat.auth.jwt.secret must not be blank");
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(30);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(14);
        }
    }
}
