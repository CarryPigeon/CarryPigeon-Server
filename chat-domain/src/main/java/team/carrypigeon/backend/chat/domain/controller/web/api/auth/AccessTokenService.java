package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.cache.CPCache;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Access Token 服务。
 * <p>
 * 提供访问令牌签发、校验、解析与撤销能力。
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final CPCache cache;

    /**
     * 签发访问令牌并写入缓存。
     *
     * @param uid 用户 ID。
     * @param ttlSeconds 令牌有效期（秒）。
     * @return 新签发的访问令牌。
     */
    public String issue(long uid, int ttlSeconds) {
        String token = "atk_" + randomToken(32);
        long expiresAt = System.currentTimeMillis() + Math.max(1, ttlSeconds) * 1000L;
        cache.set(key(token), uid + "|" + expiresAt, ttlSeconds);
        return token;
    }

    /**
     * 校验访问令牌并返回用户 ID。
     *
     * @param token 访问令牌。
     * @return 用户 ID；校验失败时返回 {@code null}。
     */
    public Long verify(String token) {
        TokenInfo info = verifyInfo(token);
        return info == null ? null : info.uid();
    }

    /**
     * 校验访问令牌并返回完整令牌信息。
     *
     * @param token 访问令牌。
     * @return 令牌信息；校验失败时返回 {@code null}。
     */
    public TokenInfo verifyInfo(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw = cache.get(key(token));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int sep = raw.indexOf('|');
        if (sep > 0) {
            String uidStr = raw.substring(0, sep);
            String expStr = raw.substring(sep + 1);
            try {
                long uid = Long.parseLong(uidStr);
                long exp = Long.parseLong(expStr);
                return new TokenInfo(uid, exp);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            long uid = Long.parseLong(raw);
            return new TokenInfo(uid, 0L);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 撤销访问令牌。
     *
     * @param token 访问令牌。
     */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        cache.delete(key(token));
    }

    /**
     * 生成令牌缓存键。
     *
     * @param token 访问令牌。
     * @return 缓存键。
     */
    private String key(String token) {
        return "cp:api:access:" + token;
    }

    /**
     * 生成随机令牌片段。
     *
     * @param bytes 随机字节长度。
     * @return Base64Url 编码后的随机串。
     */
    private String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        SECURE_RANDOM.nextBytes(b);
        return BASE64_URL.encodeToString(b);
    }

    /**
     * 访问令牌解析结果。
     *
     * @param uid 令牌绑定用户 ID。
     * @param expiresAt 过期时间戳（毫秒）。
     */
    public record TokenInfo(long uid, long expiresAt) {
    }
}
