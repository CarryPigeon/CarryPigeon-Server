package team.carrypigeon.backend.chat.domain.features.user.support.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseBackedUserProfileRepository 契约测试。
 * 职责：验证 user feature 内部仓储适配器的领域模型与 database-api 契约模型映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
@Tag("contract")
class DatabaseBackedUserProfileRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证查询命中的 database-api 记录会转换成领域模型。
     */
    @Test
    @DisplayName("find by account id existing record maps to domain model")
    void findByAccountId_existingRecord_mapsToDomainModel() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        databaseService.record = new UserProfileRecord(1001L, "carry-user", "", "", 1L, 20260420L, BASE_TIME, BASE_TIME);
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);

        Optional<UserProfile> result = repository.findByAccountId(1001L);

        assertTrue(result.isPresent());
        assertEquals(1001L, result.orElseThrow().accountId());
        assertEquals("carry-user", result.orElseThrow().nickname());
        assertEquals("", result.orElseThrow().avatarUrl());
        assertEquals("", result.orElseThrow().bio());
        assertEquals(1L, result.orElseThrow().sex());
        assertEquals(20260420L, result.orElseThrow().birthday());
    }

    /**
     * 验证保存领域模型时会转换并写入 database-api 记录。
     */
    @Test
    @DisplayName("save domain model writes database record")
    void save_domainModel_writesDatabaseRecord() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);
        UserProfile userProfile = new UserProfile(1001L, "carry-user", "", "", 1L, 20260420L, BASE_TIME, BASE_TIME);

        UserProfile result = repository.save(userProfile);

        assertEquals(userProfile, result);
        assertEquals(1001L, databaseService.insertedRecord.accountId());
        assertEquals("carry-user", databaseService.insertedRecord.nickname());
        assertEquals("", databaseService.insertedRecord.avatarUrl());
        assertEquals("", databaseService.insertedRecord.bio());
        assertEquals(1L, databaseService.insertedRecord.sex());
        assertEquals(20260420L, databaseService.insertedRecord.birthday());
    }

    /**
     * 验证更新领域模型时会转换并写入 database-api 记录。
     */
    @Test
    @DisplayName("update domain model writes database record")
    void update_domainModel_writesDatabaseRecord() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);
        UserProfile userProfile = new UserProfile(1001L, "new-name", "", "new bio", 2L, 20260421L, BASE_TIME, BASE_TIME);

        UserProfile result = repository.update(userProfile);

        assertEquals(userProfile, result);
        assertEquals(1001L, databaseService.updatedRecord.accountId());
        assertEquals("new-name", databaseService.updatedRecord.nickname());
        assertEquals("new bio", databaseService.updatedRecord.bio());
        assertEquals(2L, databaseService.updatedRecord.sex());
        assertEquals(20260421L, databaseService.updatedRecord.birthday());
    }

    /**
     * 验证游标分页查询会转换并返回领域模型列表。
     */
    @Test
    @DisplayName("find by account id before maps domain models")
    void findByAccountIdBefore_mapsDomainModels() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        databaseService.record = new UserProfileRecord(1001L, "carry-user", "", "", 0L, 0L, BASE_TIME, BASE_TIME);
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);

        java.util.List<UserProfile> result = repository.findByAccountIdBefore(1002L, 20);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).accountId());
    }

    /**
     * 验证按账户 ID 集合查询时会调用 database-api 批量查询契约。
     */
    @Test
    @DisplayName("find by account ids uses database batch query")
    void findByAccountIds_usesDatabaseBatchQuery() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        databaseService.record = new UserProfileRecord(1001L, "carry-user", "", "", 0L, 0L, BASE_TIME, BASE_TIME);
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);

        java.util.List<UserProfile> result = repository.findByAccountIds(List.of(1001L, 1002L));

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).accountId());
        assertEquals(List.of(1001L, 1002L), databaseService.batchAccountIds);
        assertEquals(0, databaseService.findAllCalls);
    }

    /**
     * 验证关键字搜索会转换并返回领域模型列表。
     */
    @Test
    @DisplayName("search by keyword maps domain models")
    void searchByKeyword_mapsDomainModels() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        databaseService.record = new UserProfileRecord(1001L, "carry-user", "", "hello carry", 0L, 0L, BASE_TIME, BASE_TIME);
        DatabaseBackedUserProfileRepository repository = new DatabaseBackedUserProfileRepository(databaseService);

        java.util.List<UserProfile> result = repository.searchByKeyword("carry", null, 20);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).accountId());
    }

    /**
     * `FakeUserProfileDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class FakeUserProfileDatabaseService implements UserProfileDatabaseService {

        private UserProfileRecord record;
        private UserProfileRecord insertedRecord;
        private UserProfileRecord updatedRecord;
        private List<Long> batchAccountIds = List.of();
        private int findAllCalls;

        @Override
        public Optional<UserProfileRecord> findByAccountId(long accountId) {
            return Optional.ofNullable(record);
        }

        @Override
        public List<UserProfileRecord> findAll() {
            findAllCalls++;
            return record == null ? List.of() : List.of(record);
        }

        @Override
        public List<UserProfileRecord> findByAccountIds(List<Long> accountIds) {
            this.batchAccountIds = accountIds;
            return record == null || !accountIds.contains(record.accountId()) ? List.of() : List.of(record);
        }

        @Override
        public List<UserProfileRecord> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return record == null || (cursorAccountId != null && record.accountId() >= cursorAccountId) ? List.of() : List.of(record);
        }

        @Override
        public List<UserProfileRecord> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            return findByAccountIdBefore(cursorAccountId, limit);
        }

        @Override
        public void insert(UserProfileRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void update(UserProfileRecord record) {
            this.updatedRecord = record;
        }
    }
}
