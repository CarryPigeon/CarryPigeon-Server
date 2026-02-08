package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.common.id.IdUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Issues and verifies long-lived refresh tokens.
 * <p>
 * Refresh token is persisted in database ({@link CPUserToken}) so that it can be revoked server-side.
 * Token format: {@code rtk_<opaque_random>}.
 * Expiration is tracked by {@link CPUserToken#getExpiredTime()}.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final UserTokenDao userTokenDao;

    public CPUserToken issue(long uid, int ttlDays) {
        CPUserToken token = new CPUserToken()
                .setId(IdUtil.generateId())
                .setUid(uid)
                .setToken("rtk_" + randomToken(48))
                .setExpiredTime(LocalDateTime.now().plusDays(Math.max(1, ttlDays)));
        userTokenDao.save(token);
        return token;
    }

    public CPUserToken getByToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return userTokenDao.getByToken(refreshToken);
    }

    public boolean isExpired(CPUserToken token) {
        return token == null || token.getExpiredTime() == null || LocalDateTime.now().isAfter(token.getExpiredTime());
    }

    public void revoke(CPUserToken token) {
        if (token == null) {
            return;
        }
        userTokenDao.delete(token);
    }

    private String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        SECURE_RANDOM.nextBytes(b);
        return BASE64_URL.encodeToString(b);
    }
}
