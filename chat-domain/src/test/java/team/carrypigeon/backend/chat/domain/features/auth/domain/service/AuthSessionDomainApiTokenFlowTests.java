package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthSessionDomainApi 令牌流契约测试。
 * 职责：验证 refresh 和 logout 的会话轮转、撤销与失败语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务编排。
 */
@Tag("contract")
class AuthSessionDomainApiTokenFlowTests {

    /**
     * 验证 refresh 成功时会撤销旧会话并签发新 token。
     */
    @Test
    @DisplayName("refresh valid token rotates session and returns tokens")
    void refresh_validToken_rotatesSessionAndReturnsTokens() {
        AuthDomainApiTestSupport.Fixture fixture = AuthDomainApiTestSupport.newFixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1)),
                false,
                AuthDomainApiTestSupport.BASE_TIME,
                AuthDomainApiTestSupport.BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1))
        );

        AuthTokenResult result = fixture.sessionApi.refresh(new RefreshTokenCommand("refresh-token-old"));

        assertEquals("access-token-1001", result.accessToken());
        assertTrue(fixture.refreshSessionRepository.findById(2001L).orElseThrow().revoked());
        assertTrue(fixture.refreshSessionRepository.findById(1001L).isPresent());
    }

    /**
     * 验证已撤销 refresh session 不能刷新 token。
     */
    @Test
    @DisplayName("refresh revoked session throws forbidden problem")
    void refresh_revokedSession_throwsForbiddenProblem() {
        AuthDomainApiTestSupport.Fixture fixture = AuthDomainApiTestSupport.newFixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1)),
                true,
                AuthDomainApiTestSupport.BASE_TIME,
                AuthDomainApiTestSupport.BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1))
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.sessionApi.refresh(new RefreshTokenCommand("refresh-token-old"))
        );

        assertEquals("refresh token is invalid", exception.getMessage());
    }

    /**
     * 验证注销会撤销 refresh token 对应的会话。
     */
    @Test
    @DisplayName("logout valid refresh token revokes session")
    void logout_validRefreshToken_revokesSession() {
        AuthDomainApiTestSupport.Fixture fixture = AuthDomainApiTestSupport.newFixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1)),
                false,
                AuthDomainApiTestSupport.BASE_TIME,
                AuthDomainApiTestSupport.BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                AuthDomainApiTestSupport.BASE_TIME.plus(Duration.ofDays(1))
        );

        fixture.sessionApi.logout(new LogoutCommand("refresh-token-old"));

        assertTrue(fixture.refreshSessionRepository.findById(2001L).orElseThrow().revoked());
    }
}
