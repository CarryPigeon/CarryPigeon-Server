package team.carrypigeon.backend.chat.domain.features.user.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.SearchUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UserProfileDomainApi 契约测试。
 * 职责：验证当前登录用户资料查询与更新用例的应用层编排契约。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class UserProfileDomainApiTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-21T12:00:00Z");
    private static final Instant UPDATED_TIME = Instant.parse("2026-04-21T12:30:00Z");

    /**
     * 验证资料存在时可以返回当前用户资料结果。
     */
    @Test
    @DisplayName("get current user profile existing profile returns result")
    void getCurrentUserProfile_existingProfile_returnsResult() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1001L, "carry-user", "https://img.example/avatar.png", "hello world", BASE_TIME));

        UserProfileResult result = fixture.service.getCurrentUserProfile(new GetCurrentUserProfileCommand(1001L));

        assertEquals(1001L, result.accountId());
        assertEquals("carry-user", result.nickname());
        assertEquals("https://img.example/avatar.png", result.avatarUrl());
        assertEquals("hello world", result.bio());
        assertEquals(BASE_TIME, result.createdAt());
        assertEquals(BASE_TIME, result.updatedAt());
    }

    /**
     * 验证资料不存在时会返回稳定的 404 问题语义。
     */
    @Test
    @DisplayName("get current user profile missing profile throws not found problem")
    void getCurrentUserProfile_missingProfile_throwsNotFoundProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getCurrentUserProfile(new GetCurrentUserProfileCommand(1001L))
        );

        assertEquals("user profile does not exist", exception.getMessage());
    }

    /**
     * 验证按账户 ID 查询资料时会返回目标用户结果。
     */
    @Test
    @DisplayName("get user profile by account id existing profile returns result")
    void getUserProfileByAccountId_existingProfile_returnsResult() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1002L, "carry-friend", "https://img.example/friend.png", "friend bio", BASE_TIME));

        UserProfileResult result = fixture.service.getUserProfileByAccountId(new GetUserProfileByAccountIdCommand(1002L));

        assertEquals(1002L, result.accountId());
        assertEquals("carry-friend", result.nickname());
        assertEquals("https://img.example/friend.png", result.avatarUrl());
        assertEquals("friend bio", result.bio());
        assertEquals(BASE_TIME, result.createdAt());
        assertEquals(BASE_TIME, result.updatedAt());
    }

    /**
     * 验证按账户 ID 查询不存在资料时会返回 404 问题语义。
     */
    @Test
    @DisplayName("get user profile by account id missing profile throws not found problem")
    void getUserProfileByAccountId_missingProfile_throwsNotFoundProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getUserProfileByAccountId(new GetUserProfileByAccountIdCommand(1002L))
        );

        assertEquals("user profile does not exist", exception.getMessage());
    }

    /**
     * 验证列表查询会返回全部用户资料结果。
     */
    @Test
    @DisplayName("list user profiles returns all results")
    void listUserProfiles_returnsAllResults() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1001L, "carry-user", "", "", BASE_TIME));
        fixture.repository.save(profile(1002L, "carry-friend", "https://img.example/friend.png", "friend bio", BASE_TIME));

        java.util.List<UserProfileResult> results = fixture.service.listUserProfiles(1001L);

        assertEquals(1, results.size());
        assertEquals(1001L, results.get(0).accountId());
    }

    /**
     * 验证分页查询会按游标返回结果并计算 nextCursor。
     */
    @Test
    @DisplayName("get user profiles with cursor returns page result")
    void getUserProfiles_withCursor_returnsPageResult() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1001L, "carry-user", "", "", BASE_TIME));
        fixture.repository.save(profile(1002L, "carry-friend", "", "friend bio", BASE_TIME));
        fixture.repository.save(profile(1003L, "carry-third", "", "third bio", BASE_TIME));

        UserProfilePageResult result = fixture.service.getUserProfiles(new GetUserProfilesQuery(1001L, 1004L, 2));

        assertEquals(2, result.users().size());
        assertEquals(1003L, result.users().get(0).accountId());
        assertEquals(1002L, result.users().get(1).accountId());
        assertEquals(1002L, result.nextCursor());
    }

    /**
     * 验证分页查询参数不合法时会返回稳定问题语义。
     */
    @Test
    @DisplayName("get user profiles invalid limit throws validation problem")
    void getUserProfiles_invalidLimit_throwsValidationProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getUserProfiles(new GetUserProfilesQuery(1001L, null, 0))
        );

        assertEquals("limit must be between 1 and 100", exception.getMessage());
    }

    /**
     * 验证搜索会按关键字过滤并返回分页结果。
     */
    @Test
    @DisplayName("search user profiles keyword returns page result")
    void searchUserProfiles_keyword_returnsPageResult() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1001L, "carry-user", "", "hello world", BASE_TIME));
        fixture.repository.save(profile(1002L, "other-user", "", "unrelated", BASE_TIME));
        fixture.repository.save(profile(1003L, "carry-friend", "", "friend bio", BASE_TIME));

        UserProfilePageResult result = fixture.service.searchUserProfiles(new SearchUserProfilesQuery(1001L, "carry", null, 10));

        assertEquals(2, result.users().size());
        assertEquals(1003L, result.users().get(0).accountId());
        assertEquals(1001L, result.users().get(1).accountId());
        assertEquals(1001L, result.nextCursor());
    }

    /**
     * 验证搜索关键字为空时会返回稳定的参数校验问题语义。
     */
    @Test
    @DisplayName("search user profiles blank keyword throws validation problem")
    void searchUserProfiles_blankKeyword_throwsValidationProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.searchUserProfiles(new SearchUserProfilesQuery(1001L, " ", null, 10))
        );

        assertEquals("keyword must not be blank", exception.getMessage());
    }

    /**
     * 验证更新成功时会保留 createdAt 并使用 TimeProvider 更新时间。
     */
    @Test
    @DisplayName("update current user profile existing profile updates fields and timestamp")
    void updateCurrentUserProfile_existingProfile_updatesFieldsAndTimestamp() {
        Fixture fixture = new Fixture();
        fixture.repository.save(profile(1001L, "old-name", "https://img.example/old.png", "old bio", BASE_TIME));

        UserProfileResult result = fixture.service.updateCurrentUserProfile(new UpdateCurrentUserProfileCommand(
                1001L,
                "new-name",
                "https://img.example/new.png",
                "new bio",
                2L,
                20260421L
        ));

        assertEquals(1001L, result.accountId());
        assertEquals("new-name", result.nickname());
        assertEquals("https://img.example/new.png", result.avatarUrl());
        assertEquals("new bio", result.bio());
        assertEquals(2L, result.sex());
        assertEquals(20260421L, result.birthday());
        assertEquals(BASE_TIME, result.createdAt());
        assertEquals(UPDATED_TIME, result.updatedAt());
        assertEquals(UPDATED_TIME, fixture.repository.findByAccountId(1001L).orElseThrow().updatedAt());
        assertEquals(2L, fixture.repository.findByAccountId(1001L).orElseThrow().sex());
        assertEquals(20260421L, fixture.repository.findByAccountId(1001L).orElseThrow().birthday());
    }

    /**
     * 验证更新不存在的资料时会返回稳定的 404 问题语义。
     */
    @Test
    @DisplayName("update current user profile missing profile throws not found problem")
    void updateCurrentUserProfile_missingProfile_throwsNotFoundProblem() {
        Fixture fixture = new Fixture();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.updateCurrentUserProfile(new UpdateCurrentUserProfileCommand(
                        1001L,
                        "new-name",
                        "https://img.example/new.png",
                        "new bio",
                        0L,
                        0L
                ))
        );

        assertEquals("user profile does not exist", exception.getMessage());
    }

    /**
     * 验证领域模型提供的默认资料工厂会生成空白可编辑字段。
     */
    @Test
    @DisplayName("user profile initial factory creates blank editable fields")
    void userProfile_initialFactory_createsBlankEditableFields() {
        UserProfile userProfile = UserProfile.initial(1001L, "carry-user", BASE_TIME, UPDATED_TIME);

        assertEquals(1001L, userProfile.accountId());
        assertEquals("carry-user", userProfile.nickname());
        assertEquals("", userProfile.avatarUrl());
        assertEquals("", userProfile.bio());
        assertEquals(0L, userProfile.sex());
        assertEquals(0L, userProfile.birthday());
        assertEquals(BASE_TIME, userProfile.createdAt());
        assertEquals(UPDATED_TIME, userProfile.updatedAt());
    }

    /**
     * 验证领域模型更新方法会保留创建时间并刷新可编辑字段与更新时间。
     */
    @Test
    @DisplayName("user profile update method preserves createdAt and refreshes editable fields")
    void userProfile_updateMethod_preservesCreatedAtAndRefreshesEditableFields() {
        UserProfile userProfile = profile(1001L, "old-name", "https://img.example/old.png", "old bio", BASE_TIME);

        UserProfile updated = userProfile.updateProfile("new-name", "https://img.example/new.png", "new bio", 1L, 20260420L, UPDATED_TIME);

        assertEquals(1001L, updated.accountId());
        assertEquals("new-name", updated.nickname());
        assertEquals("https://img.example/new.png", updated.avatarUrl());
        assertEquals("new bio", updated.bio());
        assertEquals(1L, updated.sex());
        assertEquals(20260420L, updated.birthday());
        assertEquals(BASE_TIME, updated.createdAt());
        assertEquals(UPDATED_TIME, updated.updatedAt());
    }

    private static UserProfile profile(long accountId, String nickname, String avatarUrl, String bio, Instant time) {
        return new UserProfile(accountId, nickname, avatarUrl, bio, 0L, 0L, time, time);
    }

    /**
     * `Fixture` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class Fixture {

        private final InMemoryAuthAccountRepository authAccountRepository = new InMemoryAuthAccountRepository();
        private final InMemoryUserProfileRepository repository = new InMemoryUserProfileRepository();
        private final UserProfileDomainApi service = new UserProfileDomainApi(
                authAccountRepository,
                repository,
                new NoopEmailVerificationCodeService(),
                new TimeProvider(Clock.fixed(UPDATED_TIME, ZoneOffset.UTC)),
                new NoopTransactionRunner()
        );
    }

    /**
     * `NoopEmailVerificationCodeService` 测试替身。
     * 职责：隔离验证码外部能力，使资料领域测试只关注邮箱更新编排。
     */
    private static class NoopEmailVerificationCodeService implements EmailVerificationCodeService {

        @Override
        public void issueCode(String email) {
        }

        @Override
        public void verifyCode(String email, String code) {
        }
    }

    /**
     * `InMemoryAuthAccountRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class InMemoryAuthAccountRepository implements AuthAccountRepository {

        private final Map<Long, AuthAccount> accountsById = new HashMap<>();

        @Override
        public Optional<AuthAccount> findByUsername(String username) {
            return accountsById.values().stream().filter(account -> account.username().equals(username)).findFirst();
        }

        @Override
        public Optional<AuthAccount> findById(long accountId) {
            return Optional.ofNullable(accountsById.get(accountId));
        }

        @Override
        public AuthAccount save(AuthAccount account) {
            accountsById.put(account.id(), account);
            return account;
        }

        @Override
        public AuthAccount update(AuthAccount account) {
            accountsById.put(account.id(), account);
            return account;
        }
    }

    /**
     * `InMemoryUserProfileRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profiles = new HashMap<>();

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profiles.get(accountId));
        }

        @Override
        public java.util.List<UserProfile> findAll() {
            return new java.util.ArrayList<>(profiles.values());
        }

        @Override
        public java.util.List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return profiles.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .sorted(java.util.Comparator.comparingLong(UserProfile::accountId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public java.util.List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
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
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

        @Override
        public UserProfile update(UserProfile userProfile) {
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

    }

    /**
     * `NoopTransactionRunner` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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
}
