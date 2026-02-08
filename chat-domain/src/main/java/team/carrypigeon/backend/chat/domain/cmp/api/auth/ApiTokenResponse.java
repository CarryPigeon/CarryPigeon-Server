package team.carrypigeon.backend.chat.domain.cmp.api.auth;

/**
 * Token response payload for {@code POST /api/auth/tokens} and {@code POST /api/auth/refresh}.
 */
public record ApiTokenResponse(
        String tokenType,
        String accessToken,
        int expiresIn,
        String refreshToken,
        String uid,
        boolean isNewUser
) {
    public static ApiTokenResponse from(long uid, String accessToken, int expiresIn, String refreshToken, boolean isNewUser) {
        return new ApiTokenResponse("Bearer", accessToken, expiresIn, refreshToken, Long.toString(uid), isNewUser);
    }
}

