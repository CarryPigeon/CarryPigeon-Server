package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AccessTokenAuthenticationDomainApi 契约测试。
 * 职责：验证 auth 内部 claims 会被收敛为稳定认证投影，并拒绝非法账号 subject。
 * 边界：不验证 JWT 签名算法和 HTTP 认证上下文绑定。
 */
@Tag("contract")
class AccessTokenAuthenticationDomainApiTests {

    /**
     * 验证合法 access token claims 会映射为不泄漏内部 token 字段的认证投影。
     */
    @Test
    @DisplayName("authenticate valid claims returns stable authentication result")
    void authenticate_validClaims_returnsStableAuthenticationResult() {
        Instant expiresAt = Instant.parse("2026-07-18T00:00:00Z");
        AccessTokenAuthenticationDomainApi api = new AccessTokenAuthenticationDomainApi(
                new StubAuthTokenCodec(new AuthTokenClaims("1001", "carry-user", "access", 0L, expiresAt))
        );

        AccessTokenAuthenticationResult result = api.authenticate("access-token");

        assertEquals(1001L, result.accountId());
        assertEquals("carry-user", result.username());
        assertEquals(expiresAt, result.expiresAt());
    }

    /**
     * 验证非数字 subject 不会穿透到协议层，而是转换为稳定的非法 access token 问题。
     */
    @Test
    @DisplayName("authenticate non numeric subject throws invalid access token problem")
    void authenticate_nonNumericSubject_throwsInvalidAccessTokenProblem() {
        AccessTokenAuthenticationDomainApi api = new AccessTokenAuthenticationDomainApi(
                new StubAuthTokenCodec(new AuthTokenClaims("invalid", "carry-user", "access", 0L, Instant.MAX))
        );

        ProblemException exception = assertThrows(ProblemException.class, () -> api.authenticate("access-token"));

        assertEquals("invalid_access_token", exception.reason());
    }

    private static final class StubAuthTokenCodec implements AuthTokenCodec {

        private final AuthTokenClaims claims;

        private StubAuthTokenCodec(AuthTokenClaims claims) {
            this.claims = claims;
        }

        @Override
        public String issueAccessToken(AuthAccount account, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            return claims;
        }

        @Override
        public AuthTokenClaims parseRefreshToken(String refreshToken) {
            throw new UnsupportedOperationException();
        }
    }
}
