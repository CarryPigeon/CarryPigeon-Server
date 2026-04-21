package team.carrypigeon.backend.chat.domain.features.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthApplicationService 契约测试。
 * 职责：验证注册、登录、刷新和注销用例的应用层编排契约。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
class AuthApplicationServiceTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-20T12:00:00Z");

    /**
     * 验证注册成功时会返回账户结果、保存哈希后的密码并初始化默认资料。
     */
    @Test
    @DisplayName("register new account returns result stores hashed password and provisions default profile")
    void register_newAccount_returnsResultStoresHashedPasswordAndProvisionsDefaultProfile() {
        Fixture fixture = new Fixture();

        RegisterResult result = fixture.service.register(new RegisterCommand("carry-user", "password123"));

        assertEquals(1001L, result.accountId());
        assertEquals("carry-user", result.username());
        assertTrue(fixture.accountRepository.findByUsername("carry-user").isPresent());
        assertNotEquals("password123", fixture.accountRepository.findByUsername("carry-user").orElseThrow().passwordHash());
        UserProfile userProfile = fixture.userProfileRepository.findByAccountId(1001L).orElseThrow();
        assertEquals("carry-user", userProfile.nickname());
        assertEquals("", userProfile.avatarUrl());
        assertEquals("", userProfile.bio());
    }

    /**
     * 验证用户名重复时会中断注册并返回参数类问题语义。
     */
    @Test
    @DisplayName("register duplicate username throws validation problem")
    void register_duplicateUsername_throwsValidationProblem() {
        Fixture fixture = new Fixture();
        fixture.accountRepository.save(account(1L, "carry-user", "hashed::existing"));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.register(new RegisterCommand("carry-user", "password123"))
        );

        assertEquals("username already exists", exception.getMessage());
    }

    /**
     * 验证默认资料初始化失败时注册事务会整体回滚，避免留下没有资料的账户。
     */
    @Test
    @DisplayName("register profile provisioning failure rolls back account creation")
    void register_profileProvisioningFailure_rollsBackAccountCreation() {
        InMemoryAuthAccountRepository accountRepository = new InMemoryAuthAccountRepository();
        InMemoryAuthRefreshSessionRepository refreshSessionRepository = new InMemoryAuthRefreshSessionRepository();
        InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        userProfileRepository.failOnSave = true;
        AuthApplicationService service = service(
                accountRepository,
                refreshSessionRepository,
                userProfileRepository,
                new FakeAuthTokenService(),
                new SnapshotTransactionRunner(accountRepository, userProfileRepository)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.register(new RegisterCommand("carry-user", "password123"))
        );

        assertEquals("profile provisioning failed", exception.getMessage());
        assertFalse(accountRepository.findByUsername("carry-user").isPresent());
        assertFalse(userProfileRepository.findByAccountId(1001L).isPresent());
    }

    /**
     * 验证登录成功时会签发 access token、refresh token 并保存 refresh session。
     */
    @Test
    @DisplayName("login valid credentials returns tokens and stores refresh session")
    void login_validCredentials_returnsTokensAndStoresRefreshSession() {
        Fixture fixture = new Fixture();
        fixture.accountRepository.save(account(1001L, "carry-user", "hashed::password123"));

        AuthTokenResult result = fixture.service.login(new LoginCommand("carry-user", "password123"));

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
        Fixture fixture = new Fixture();
        fixture.accountRepository.save(account(1001L, "carry-user", "hashed::password123"));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.login(new LoginCommand("carry-user", "wrong-password"))
        );

        assertEquals("username or password is invalid", exception.getMessage());
    }

    /**
     * 验证账号不存在时登录会返回认证失败语义。
     */
    @Test
    @DisplayName("login missing account throws forbidden problem")
    void login_missingAccount_throwsForbiddenProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.login(new LoginCommand("missing-user", "password123"))
        );

        assertEquals("username or password is invalid", exception.getMessage());
    }

    /**
     * 验证 refresh 成功时会撤销旧会话并签发新 token。
     */
    @Test
    @DisplayName("refresh valid token rotates session and returns tokens")
    void refresh_validToken_rotatesSessionAndReturnsTokens() {
        Fixture fixture = new Fixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                BASE_TIME.plus(Duration.ofDays(1)),
                false,
                BASE_TIME,
                BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                BASE_TIME.plus(Duration.ofDays(1))
        );

        AuthTokenResult result = fixture.service.refresh(new RefreshTokenCommand("refresh-token-old"));

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
        Fixture fixture = new Fixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                BASE_TIME.plus(Duration.ofDays(1)),
                true,
                BASE_TIME,
                BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                BASE_TIME.plus(Duration.ofDays(1))
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.refresh(new RefreshTokenCommand("refresh-token-old"))
        );

        assertEquals("refresh token is invalid", exception.getMessage());
    }

    /**
     * 验证注销会撤销 refresh token 对应的会话。
     */
    @Test
    @DisplayName("logout valid refresh token revokes session")
    void logout_validRefreshToken_revokesSession() {
        Fixture fixture = new Fixture();
        fixture.refreshSessionRepository.save(new AuthRefreshSession(
                2001L,
                1001L,
                "hash::refresh-token-old",
                BASE_TIME.plus(Duration.ofDays(1)),
                false,
                BASE_TIME,
                BASE_TIME
        ));
        fixture.tokenService.refreshClaims = new AuthTokenClaims(
                "1001",
                "carry-user",
                "refresh",
                2001L,
                BASE_TIME.plus(Duration.ofDays(1))
        );

        fixture.service.logout(new LogoutCommand("refresh-token-old"));

        assertTrue(fixture.refreshSessionRepository.findById(2001L).orElseThrow().revoked());
    }

    private static AuthAccount account(long id, String username, String passwordHash) {
        return new AuthAccount(id, username, passwordHash, BASE_TIME, BASE_TIME);
    }

    private static class Fixture {

        private final InMemoryAuthAccountRepository accountRepository = new InMemoryAuthAccountRepository();
        private final InMemoryAuthRefreshSessionRepository refreshSessionRepository = new InMemoryAuthRefreshSessionRepository();
        private final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        private final FakeAuthTokenService tokenService = new FakeAuthTokenService();
        private final AuthApplicationService service = service(
                accountRepository,
                refreshSessionRepository,
                userProfileRepository,
                tokenService,
                new NoopTransactionRunner()
        );
    }

    private static AuthApplicationService service(
            AuthAccountRepository accountRepository,
            AuthRefreshSessionRepository refreshSessionRepository,
            UserProfileRepository userProfileRepository,
            AuthTokenService authTokenService,
            TransactionRunner transactionRunner
    ) {
        return new AuthApplicationService(
                accountRepository,
                refreshSessionRepository,
                userProfileRepository,
                new PrefixPasswordHasher(),
                token -> "hash::" + token,
                authTokenService,
                new AuthJwtProperties(
                        "test-issuer",
                        "test-secret-test-secret-test-secret",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)
                ),
                new IncrementingIdGenerator(),
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                transactionRunner
        );
    }

    private static class InMemoryAuthAccountRepository implements AuthAccountRepository {

        private final Map<String, AuthAccount> accounts = new HashMap<>();

        @Override
        public Optional<AuthAccount> findByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public AuthAccount save(AuthAccount account) {
            accounts.put(account.username(), account);
            return account;
        }

        private Map<String, AuthAccount> snapshot() {
            return new HashMap<>(accounts);
        }

        private void restore(Map<String, AuthAccount> snapshot) {
            accounts.clear();
            accounts.putAll(snapshot);
        }
    }

    private static class InMemoryAuthRefreshSessionRepository implements AuthRefreshSessionRepository {

        private final Map<Long, AuthRefreshSession> sessions = new HashMap<>();

        @Override
        public Optional<AuthRefreshSession> findById(long sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public AuthRefreshSession save(AuthRefreshSession session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public void revoke(long sessionId) {
            AuthRefreshSession session = sessions.get(sessionId);
            sessions.put(sessionId, new AuthRefreshSession(
                    session.id(),
                    session.accountId(),
                    session.refreshTokenHash(),
                    session.expiresAt(),
                    true,
                    session.createdAt(),
                    BASE_TIME
            ));
        }
    }

    private static class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profiles = new HashMap<>();
        private boolean failOnSave;

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profiles.get(accountId));
        }

        @Override
        public UserProfile save(UserProfile userProfile) {
            if (failOnSave) {
                throw new IllegalStateException("profile provisioning failed");
            }
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

        @Override
        public UserProfile update(UserProfile userProfile) {
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

        private Map<Long, UserProfile> snapshot() {
            return new HashMap<>(profiles);
        }

        private void restore(Map<Long, UserProfile> snapshot) {
            profiles.clear();
            profiles.putAll(snapshot);
        }
    }

    private static class PrefixPasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed::" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return hash(rawPassword).equals(passwordHash);
        }
    }

    private static class FakeAuthTokenService implements AuthTokenService {

        private AuthTokenClaims refreshClaims;

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            return new AuthTokenClaims("1001", "carry-user", "access", 0L, BASE_TIME.plus(Duration.ofMinutes(30)));
        }

        @Override
        public String issueAccessToken(AuthAccount account, Instant expiresAt) {
            return "access-token-" + account.id();
        }

        @Override
        public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
            return "refresh-token-" + account.id() + "-" + refreshSessionId;
        }

        @Override
        public AuthTokenClaims parseRefreshToken(String refreshToken) {
            return refreshClaims;
        }
    }

    private static class IncrementingIdGenerator implements team.carrypigeon.backend.infrastructure.basic.id.IdGenerator {

        private long next = 1001L;

        @Override
        public long nextLongId() {
            return next++;
        }
    }

    private static class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    private static class SnapshotTransactionRunner implements TransactionRunner {

        private final InMemoryAuthAccountRepository accountRepository;
        private final InMemoryUserProfileRepository userProfileRepository;

        private SnapshotTransactionRunner(
                InMemoryAuthAccountRepository accountRepository,
                InMemoryUserProfileRepository userProfileRepository
        ) {
            this.accountRepository = accountRepository;
            this.userProfileRepository = userProfileRepository;
        }

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            Map<String, AuthAccount> accountSnapshot = accountRepository.snapshot();
            Map<Long, UserProfile> profileSnapshot = userProfileRepository.snapshot();
            try {
                return action.get();
            } catch (RuntimeException exception) {
                accountRepository.restore(accountSnapshot);
                userProfileRepository.restore(profileSnapshot);
                throw exception;
            }
        }

        @Override
        public void runInTransaction(Runnable action) {
            runInTransaction(() -> {
                action.run();
                return null;
            });
        }
    }
}
