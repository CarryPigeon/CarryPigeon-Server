package team.carrypigeon.backend.chat.domain.features.auth.domain.projection;

import java.time.Instant;

/**
 * Access token 认证结果。
 *
 * @param accountId 已认证账号 ID
 * @param username 已认证账号名
 * @param expiresAt access token 过期时间
 */
public record AccessTokenAuthenticationResult(long accountId, String username, Instant expiresAt) {
}
