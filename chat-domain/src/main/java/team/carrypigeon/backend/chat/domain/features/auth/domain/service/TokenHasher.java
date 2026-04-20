package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

/**
 * 令牌摘要服务。
 * 职责：为 refresh token 持久化提供不可逆摘要能力。
 * 边界：不负责 JWT 签发与解析。
 */
public interface TokenHasher {

    /**
     * 生成令牌摘要。
     *
     * @param token 原始令牌
     * @return 令牌摘要
     */
    String hash(String token);
}
