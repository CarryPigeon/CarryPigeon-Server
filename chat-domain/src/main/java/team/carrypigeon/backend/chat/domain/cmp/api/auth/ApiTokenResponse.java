package team.carrypigeon.backend.chat.domain.cmp.api.auth;

/**
 * 令牌接口响应体。
 * <p>
 * 适用于：`POST /api/auth/tokens` 与 `POST /api/auth/refresh`。
 */
public record ApiTokenResponse(
        String tokenType,
        String accessToken,
        int expiresIn,
        String refreshToken,
        String uid,
        boolean isNewUser
) {

    /**
     * 构造标准 Bearer 令牌响应。
     */
    public static ApiTokenResponse from(long uid, String accessToken, int expiresIn, String refreshToken, boolean isNewUser) {
        return new ApiTokenResponse("Bearer", accessToken, expiresIn, refreshToken, Long.toString(uid), isNewUser);
    }
}
