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
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetCurrentUserProfileQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
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

        UserProfileResult result = fixture.service.getCurrentUserProfile(new GetCurrentUserProfileQuery(1001L));

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
                () -> fixture.service.getCurrentUserProfile(new GetCurrentUserProfileQuery(1001L))
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

        UserProfileResult result = fixture.service.getUserProfileByAccountId(new GetUserProfileByAccountIdQuery(1002L));

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
                () -> fixture.service.getUserProfileByAccountId(new GetUserProfileByAccountIdQuery(1002L))
        );

        assertEquals("user profile does not exist", exception.getMessage());
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

        private final InMemoryUserProfileRepository repository = new InMemoryUserProfileRepository();
        private final UserProfileDomainApi service = new UserProfileDomainApi(
                repository,
                new TimeProviderImpl(Clock.fixed(UPDATED_TIME, ZoneOffset.UTC)),
                new NoopTransactionRunner()
        );
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
        public java.util.List<UserProfile> findByAccountIds(java.util.List<Long> accountIds) {
            return profiles.values().stream()
                    .filter(profile -> accountIds.contains(profile.accountId()))
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
