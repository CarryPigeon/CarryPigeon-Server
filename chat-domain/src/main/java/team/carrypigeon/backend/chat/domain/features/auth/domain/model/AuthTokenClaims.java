package team.carrypigeon.backend.chat.domain.features.auth.domain.model;

import java.time.Instant;

/**
 * 鉴权令牌声明。
 * 职责：表达本服务端签发 JWT 后可被业务识别的最小声明。
 * 边界：不表达客户端存储方式与跨服务端身份语义。
 *
 * @param subject 账户 ID 字符串
 * @param username 当前服务端用户名
 * @param tokenType 令牌类型
 * @param sessionId refresh token 绑定的会话 ID
 * @param expiresAt 过期时间
 */
public record AuthTokenClaims(
        String subject,
        String username,
        String tokenType,
        long sessionId,
        Instant expiresAt
) {
}
