package team.carrypigeon.backend.chat.domain.features.auth.application.dto;

/**
 * 会话令牌应用结果。
 * 职责：向 v1 公共鉴权协议返回最小 token 会话结果。
 * 边界：不承载用户资料与权限语义。
 */
public record AuthSessionTokenResult(
        long accountId,
        String accessToken,
        long expiresIn,
        String refreshToken,
        boolean newUser
) {
}
