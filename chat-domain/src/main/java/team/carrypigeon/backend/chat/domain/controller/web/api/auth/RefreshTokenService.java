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
 * Refresh Token 服务。
 * <p>
 * 提供刷新令牌签发、查询、过期判断与吊销能力。
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final UserTokenDao userTokenDao;

    /**
     * 签发刷新令牌并持久化。
     *
     * @param uid 用户 ID。
     * @param ttlDays 令牌有效期（天）。
     * @return 持久化后的刷新令牌实体。
     */
    public CPUserToken issue(long uid, int ttlDays) {
        CPUserToken token = new CPUserToken()
                .setId(IdUtil.generateId())
                .setUid(uid)
                .setToken("rtk_" + randomToken(48))
                .setExpiredTime(LocalDateTime.now().plusDays(Math.max(1, ttlDays)));
        userTokenDao.save(token);
        return token;
    }

    /**
     * 按令牌字符串查询刷新令牌记录。
     *
     * @param refreshToken 刷新令牌字符串。
     * @return 刷新令牌记录；不存在时返回 {@code null}。
     */
    public CPUserToken getByToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return userTokenDao.getByToken(refreshToken);
    }

    /**
     * 判断刷新令牌是否过期。
     *
     * @param token 刷新令牌实体。
     * @return 已过期返回 {@code true}，否则返回 {@code false}。
     */
    public boolean isExpired(CPUserToken token) {
        return token == null || token.getExpiredTime() == null || LocalDateTime.now().isAfter(token.getExpiredTime());
    }

    /**
     * 吊销刷新令牌。
     *
     * @param token 待吊销的刷新令牌实体。
     */
    public void revoke(CPUserToken token) {
        if (token == null) {
            return;
        }
        userTokenDao.delete(token);
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
}
