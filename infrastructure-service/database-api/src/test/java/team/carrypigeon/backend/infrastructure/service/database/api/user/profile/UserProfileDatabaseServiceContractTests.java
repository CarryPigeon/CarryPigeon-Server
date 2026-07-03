package team.carrypigeon.backend.infrastructure.service.database.api.user.profile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserProfileDatabaseService 行为契约测试。
 * 职责：锁定用户资料数据库抽象在查询、分页、搜索、写入与更新场景下的最小稳定语义。
 * 边界：只验证接口文档层面的资料记录契约，不依赖具体数据库实现。
 */
@Tag("contract")
class UserProfileDatabaseServiceContractTests {

    private static final UserProfileRecord RECORD = new UserProfileRecord(
            1001L,
            "carry-user",
            "https://img.example/avatar.png",
            "hello world",
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-21T12:00:00Z")
    );

    /**
     * 验证已写入资料可通过账户 ID 查询命中同一快照。
     */
    @Test
    @DisplayName("find by account id inserted profile returns snapshot")
    void findByAccountId_insertedProfile_returnsSnapshot() {
        InMemoryUserProfileDatabaseService service = new InMemoryUserProfileDatabaseService();
        service.insert(RECORD);

        Optional<UserProfileRecord> result = service.findByAccountId(1001L);

        assertEquals(Optional.of(RECORD), result);
    }

    /**
     * 验证游标分页查询只返回游标之前的资料记录。
     */
    @Test
    @DisplayName("find by account id before filters profiles before cursor")
    void findByAccountIdBefore_filtersProfilesBeforeCursor() {
        InMemoryUserProfileDatabaseService service = new InMemoryUserProfileDatabaseService();
        service.insert(RECORD);
        UserProfileRecord olderRecord = new UserProfileRecord(
                1000L,
                "carry-old",
                "",
                "archive",
                RECORD.createdAt(),
                RECORD.updatedAt()
        );
        service.insert(olderRecord);

        List<UserProfileRecord> result = service.findByAccountIdBefore(1001L, 10);

        assertEquals(List.of(olderRecord), result);
    }

    /**
     * 验证关键字搜索会匹配昵称和简介中的命中资料。
     */
    @Test
    @DisplayName("search by keyword matches nickname or bio")
    void searchByKeyword_matchesNicknameOrBio() {
        InMemoryUserProfileDatabaseService service = new InMemoryUserProfileDatabaseService();
        service.insert(RECORD);
        service.insert(new UserProfileRecord(
                1002L,
                "plain-user",
                "",
                "carry project member",
                RECORD.createdAt(),
                RECORD.updatedAt()
        ));

        List<UserProfileRecord> result = service.searchByKeyword("carry", null, 10);

        assertEquals(2, result.size());
    }

    /**
     * 验证更新后后续查询会观察到新的资料快照。
     */
    @Test
    @DisplayName("update existing profile replaces stored snapshot")
    void update_existingProfile_replacesStoredSnapshot() {
        InMemoryUserProfileDatabaseService service = new InMemoryUserProfileDatabaseService();
        service.insert(RECORD);
        UserProfileRecord updatedRecord = new UserProfileRecord(
                1001L,
                "carry-user-v2",
                "https://img.example/avatar-v2.png",
                "updated bio",
                RECORD.createdAt(),
                Instant.parse("2026-04-22T12:00:00Z")
        );

        service.update(updatedRecord);

        assertEquals(Optional.of(updatedRecord), service.findByAccountId(1001L));
    }

    /**
     * 验证未写入资料时查询返回空。
     */
    @Test
    @DisplayName("find by account id missing profile returns empty optional")
    void findByAccountId_missingProfile_returnsEmptyOptional() {
        UserProfileDatabaseService service = new InMemoryUserProfileDatabaseService();

        Optional<UserProfileRecord> result = service.findByAccountId(1001L);

        assertTrue(result.isEmpty());
    }

    /**
     * `InMemoryUserProfileDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class InMemoryUserProfileDatabaseService implements UserProfileDatabaseService {

        private final Map<Long, UserProfileRecord> recordsByAccountId = new HashMap<>();

        @Override
        public Optional<UserProfileRecord> findByAccountId(long accountId) {
            return Optional.ofNullable(recordsByAccountId.get(accountId));
        }

        @Override
        public List<UserProfileRecord> findAll() {
            return recordsByAccountId.values().stream()
                    .sorted(Comparator.comparingLong(UserProfileRecord::accountId))
                    .toList();
        }

        @Override
        public List<UserProfileRecord> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return recordsByAccountId.values().stream()
                    .filter(record -> cursorAccountId == null || record.accountId() < cursorAccountId)
                    .sorted(Comparator.comparingLong(UserProfileRecord::accountId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<UserProfileRecord> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            List<UserProfileRecord> results = new ArrayList<>();
            for (UserProfileRecord record : recordsByAccountId.values()) {
                boolean beforeCursor = cursorAccountId == null || record.accountId() < cursorAccountId;
                boolean matched = record.nickname().contains(normalizedKeyword) || record.bio().contains(normalizedKeyword);
                if (beforeCursor && matched) {
                    results.add(record);
                }
            }
            results.sort(Comparator.comparingLong(UserProfileRecord::accountId).reversed());
            return results.stream().limit(limit).toList();
        }

        @Override
        public void insert(UserProfileRecord record) {
            recordsByAccountId.put(record.accountId(), record);
        }

        @Override
        public void update(UserProfileRecord record) {
            insert(record);
        }
    }
}
