package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Duration;

/**
 * 鉴权 token 签发设置。
 * 职责：向领域服务提供签发 token 所需的最小时间参数。
 * 边界：不承载 JWT issuer、secret 或配置绑定细节。
 *
 * @param accessTokenTtl access token 有效期
 * @param refreshTokenTtl refresh token 有效期
 */
public record AuthTokenSettings(Duration accessTokenTtl, Duration refreshTokenTtl) {

    public AuthTokenSettings {
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("accessTokenTtl must be positive");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("refreshTokenTtl must be positive");
        }
    }
}
