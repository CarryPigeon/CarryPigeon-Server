package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.cache.CPCache;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Issues and verifies short-lived access tokens for HTTP APIs.
 * <p>
 * Token format: {@code atk_<opaque_random>} (URL-safe Base64 without padding).
 * Storage: {@link CPCache} (typically Redis) with TTL.
 * <p>
 * This is intentionally stateless on the server-side (opaque token lookup).
 * If you need JWT-style self-contained tokens, implement a separate strategy.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final CPCache cache;

    public String issue(long uid, int ttlSeconds) {
        String token = "atk_" + randomToken(32);
        long expiresAt = System.currentTimeMillis() + Math.max(1, ttlSeconds) * 1000L;
        // Value format (v2): "<uid>|<expiresAtMillis>".
        // Backward compatible with v1: "<uid>" only.
        cache.set(key(token), uid + "|" + expiresAt, ttlSeconds);
        return token;
    }

    public Long verify(String token) {
        TokenInfo info = verifyInfo(token);
        return info == null ? null : info.uid();
    }

    public TokenInfo verifyInfo(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw = cache.get(key(token));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // v2: "<uid>|<expiresAtMillis>"
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
        // v1: "<uid>"
        try {
            long uid = Long.parseLong(raw);
            return new TokenInfo(uid, 0L);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        cache.delete(key(token));
    }

    private String key(String token) {
        return "cp:api:access:" + token;
    }

    private String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        SECURE_RANDOM.nextBytes(b);
        return BASE64_URL.encodeToString(b);
    }

    public record TokenInfo(long uid, long expiresAt) {
    }
}
