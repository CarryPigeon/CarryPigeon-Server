package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 鉴权领域 API 测试支撑。
 * 职责：为注册、认证、令牌流测试提供固定时间、内存仓储替身和最小装配入口。
 * 边界：仅供当前测试包使用，不承载正式业务规则。
 */
final class AuthDomainApiTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-04-20T12:00:00Z");

    private AuthDomainApiTestSupport() {
    }

    static Fixture newFixture() {
        return new Fixture();
    }

    static AuthAccount account(long id, String username, String passwordHash) {
        return new AuthAccount(id, username, passwordHash, BASE_TIME, BASE_TIME);
    }

    static AuthAccountDomainApi createAccountApi(
            AuthAccountRepository accountRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            TransactionRunner transactionRunner
    ) {
        return new AuthAccountDomainApi(
                accountRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                new PrefixPasswordHasher(),
                new IncrementingIdGenerator(),
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                transactionRunner
        );
    }

    static AuthSessionDomainApi createSessionApi(
            AuthAccountRepository accountRepository,
            AuthRefreshSessionRepository refreshSessionRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            AuthTokenService authTokenService,
            TransactionRunner transactionRunner
    ) {
        return new AuthSessionDomainApi(
                accountRepository,
                refreshSessionRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                new PrefixPasswordHasher(),
                token -> "hash::" + token,
                authTokenService,
                new AuthTokenSettings(
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)
                ),
                new AuthPasswordLoginPolicy(true),
                new IncrementingIdGenerator(),
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                transactionRunner,
                new NoopEmailVerificationCodeService()
        );
    }

    /**
     * `NoopEmailVerificationCodeService` 验证码端口替身。
     * 职责：让会话领域 API 测试显式装配验证码边界，不验证发送和校验实现。
     */
    private static final class NoopEmailVerificationCodeService implements EmailVerificationCodeService {

        @Override
        public void issueCode(String email) {
        }

        @Override
        public void verifyCode(String email, String code) {
        }
    }

    /**
     * `Fixture` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class Fixture {

        final InMemoryAuthAccountRepository accountRepository = new InMemoryAuthAccountRepository();
        final InMemoryAuthRefreshSessionRepository refreshSessionRepository = new InMemoryAuthRefreshSessionRepository();
        final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        final FakeAuthTokenService tokenService = new FakeAuthTokenService();
        final AuthAccountDomainApi accountApi = createAccountApi(
                accountRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                new NoopTransactionRunner()
        );
        final AuthSessionDomainApi sessionApi = createSessionApi(
                accountRepository,
                refreshSessionRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                tokenService,
                new NoopTransactionRunner()
        );
    }

    /**
     * `InMemoryAuthAccountRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryAuthAccountRepository implements AuthAccountRepository {

        private final Map<String, AuthAccount> accounts = new HashMap<>();

        @Override
        public Optional<AuthAccount> findByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public Optional<AuthAccount> findById(long accountId) {
            return accounts.values().stream().filter(account -> account.id() == accountId).findFirst();
        }

        @Override
        public AuthAccount save(AuthAccount account) {
            accounts.put(account.username(), account);
            return account;
        }

        @Override
        public AuthAccount update(AuthAccount account) {
            accounts.values().removeIf(existing -> existing.id() == account.id());
            accounts.put(account.username(), account);
            return account;
        }

        Map<String, AuthAccount> snapshot() {
            return new HashMap<>(accounts);
        }

        void restore(Map<String, AuthAccount> snapshot) {
            accounts.clear();
            accounts.putAll(snapshot);
        }
    }

    /**
     * `InMemoryAuthRefreshSessionRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryAuthRefreshSessionRepository implements AuthRefreshSessionRepository {

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

    /**
     * `InMemoryUserProfileRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profiles = new HashMap<>();
        boolean failOnSave;

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profiles.get(accountId));
        }

        @Override
        public List<UserProfile> findAll() {
            return new java.util.ArrayList<>(profiles.values());
        }

        @Override
        public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return profiles.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .sorted(java.util.Comparator.comparingLong(UserProfile::accountId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            return profiles.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .filter(profile -> profile.nickname().contains(normalizedKeyword) || profile.bio().contains(normalizedKeyword))
                    .sorted(java.util.Comparator.comparingLong(UserProfile::accountId).reversed())
                    .limit(limit)
                    .toList();
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

        Map<Long, UserProfile> snapshot() {
            return new HashMap<>(profiles);
        }

        void restore(Map<Long, UserProfile> snapshot) {
            profiles.clear();
            profiles.putAll(snapshot);
        }
    }

    /**
     * `InMemoryChannelRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryChannelRepository implements ChannelRepository {

        final Map<Long, Channel> channels = new HashMap<>();
        final List<Channel> savedChannels = new java.util.ArrayList<>();

        InMemoryChannelRepository() {
            channels.put(1L, new Channel(1L, 1L, "public", "", "", "", "public", true, BASE_TIME, BASE_TIME));
            channels.put(2L, new Channel(2L, 2L, "system", "", "", "", "system", false, BASE_TIME, BASE_TIME));
        }

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channels.get(1L));
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return channels.values().stream().filter(channel -> "system".equals(channel.type())).findFirst();
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }

        @Override
        public Channel save(Channel channel) {
            savedChannels.add(channel);
            channels.put(channel.id(), channel);
            return channel;
        }
    }

    /**
     * `InMemoryChannelMemberRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        private final Map<Long, List<Long>> membersByChannelId = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).contains(accountId);
        }

        @Override
        public void save(ChannelMember channelMember) {
            membersByChannelId.computeIfAbsent(channelMember.channelId(), ignored -> new java.util.ArrayList<>())
                    .add(channelMember.accountId());
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return membersByChannelId.getOrDefault(channelId, List.of());
        }

        Map<Long, List<Long>> snapshot() {
            Map<Long, List<Long>> snapshot = new HashMap<>();
            membersByChannelId.forEach((channelId, accountIds) -> snapshot.put(channelId, new java.util.ArrayList<>(accountIds)));
            return snapshot;
        }

        void restore(Map<Long, List<Long>> snapshot) {
            membersByChannelId.clear();
            snapshot.forEach((channelId, accountIds) -> membersByChannelId.put(channelId, new java.util.ArrayList<>(accountIds)));
        }
    }

    /**
     * `PrefixPasswordHasher` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class PrefixPasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed::" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return hash(rawPassword).equals(passwordHash);
        }
    }

    /**
     * `FakeAuthTokenService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class FakeAuthTokenService implements AuthTokenService {

        AuthTokenClaims refreshClaims;

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

    /**
     * `IncrementingIdGenerator` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class IncrementingIdGenerator implements team.carrypigeon.backend.infrastructure.basic.id.IdGenerator {

        private long next = 1001L;

        @Override
        public long nextLongId() {
            return next++;
        }
    }

    /**
     * `NoopTransactionRunner` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    /**
     * `SnapshotTransactionRunner` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class SnapshotTransactionRunner implements TransactionRunner {

        private final InMemoryAuthAccountRepository accountRepository;
        private final InMemoryUserProfileRepository userProfileRepository;
        private final InMemoryChannelMemberRepository channelMemberRepository;

        SnapshotTransactionRunner(
                InMemoryAuthAccountRepository accountRepository,
                InMemoryUserProfileRepository userProfileRepository,
                InMemoryChannelMemberRepository channelMemberRepository
        ) {
            this.accountRepository = accountRepository;
            this.userProfileRepository = userProfileRepository;
            this.channelMemberRepository = channelMemberRepository;
        }

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            Map<String, AuthAccount> accountSnapshot = accountRepository.snapshot();
            Map<Long, UserProfile> profileSnapshot = userProfileRepository.snapshot();
            Map<Long, List<Long>> channelMemberSnapshot = channelMemberRepository.snapshot();
            try {
                return action.get();
            } catch (RuntimeException exception) {
                accountRepository.restore(accountSnapshot);
                userProfileRepository.restore(profileSnapshot);
                channelMemberRepository.restore(channelMemberSnapshot);
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
