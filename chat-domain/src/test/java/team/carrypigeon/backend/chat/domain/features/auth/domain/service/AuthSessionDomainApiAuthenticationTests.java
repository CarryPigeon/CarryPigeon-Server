package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainApiTestSupport.account;
import static team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainApiTestSupport.newFixture;

/**
 * AuthSessionDomainApi 认证契约测试。
 * 职责：验证登录用例的成功与失败语义，以及 refresh session 持久化编排。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务编排。
 */
@Tag("contract")
class AuthSessionDomainApiAuthenticationTests {

    /**
     * 验证登录成功时会签发 access token、refresh token 并保存 refresh session。
     */
    @Test
    @DisplayName("login valid credentials returns tokens and stores refresh session")
    void login_validCredentials_returnsTokensAndStoresRefreshSession() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();
        fixture.accountRepository.save(account(1001L, "carry-user", "hashed::password123"));

        AuthTokenResult result = fixture.sessionApi.login(new LoginCommand("carry-user", "password123"));

        assertEquals(1001L, result.accountId());
        assertEquals("carry-user", result.username());
        assertEquals("access-token-1001", result.accessToken());
        assertEquals("refresh-token-1001-1001", result.refreshToken());
        assertTrue(fixture.refreshSessionRepository.findById(1001L).isPresent());
    }

    /**
     * 验证密码错误时登录会返回认证失败语义。
     */
    @Test
    @DisplayName("login invalid password throws forbidden problem")
    void login_invalidPassword_throwsForbiddenProblem() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();
        fixture.accountRepository.save(account(1001L, "carry-user", "hashed::password123"));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.sessionApi.login(new LoginCommand("carry-user", "wrong-password"))
        );

        assertEquals("username or password is invalid", exception.getMessage());
    }

    /**
     * 验证账号不存在时登录会返回认证失败语义。
     */
    @Test
    @DisplayName("login missing account throws forbidden problem")
    void login_missingAccount_throwsForbiddenProblem() {
        AuthDomainApiTestSupport.Fixture fixture = newFixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.sessionApi.login(new LoginCommand("missing-user", "password123"))
        );

        assertEquals("username or password is invalid", exception.getMessage());
    }
}
