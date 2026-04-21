package team.carrypigeon.backend.chat.domain.features.user.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UserProfileApplicationService 契约测试。
 * 职责：验证当前登录用户资料查询与更新用例的应用层编排契约。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
class UserProfileApplicationServiceTests {

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
                "new bio"
        ));

        assertEquals(1001L, result.accountId());
        assertEquals("new-name", result.nickname());
        assertEquals("https://img.example/new.png", result.avatarUrl());
        assertEquals("new bio", result.bio());
        assertEquals(BASE_TIME, result.createdAt());
        assertEquals(UPDATED_TIME, result.updatedAt());
        assertEquals(UPDATED_TIME, fixture.repository.findByAccountId(1001L).orElseThrow().updatedAt());
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
                        "new bio"
                ))
        );

        assertEquals("user profile does not exist", exception.getMessage());
    }

    private static UserProfile profile(long accountId, String nickname, String avatarUrl, String bio, Instant time) {
        return new UserProfile(accountId, nickname, avatarUrl, bio, time, time);
    }

    private static class Fixture {

        private final InMemoryUserProfileRepository repository = new InMemoryUserProfileRepository();
        private final UserProfileApplicationService service = new UserProfileApplicationService(
                repository,
                new TimeProvider(Clock.fixed(UPDATED_TIME, ZoneOffset.UTC)),
                new NoopTransactionRunner()
        );
    }

    private static class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profiles = new HashMap<>();

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profiles.get(accountId));
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
