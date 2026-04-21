package team.carrypigeon.backend.starter.config;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StarterUserProfileRepository 契约测试。
 * 职责：验证 starter 层用户资料仓储适配器的领域模型与 database-api 契约模型映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
class StarterUserProfileRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证查询命中的 database-api 记录会转换成领域模型。
     */
    @Test
    @DisplayName("find by account id existing record maps to domain model")
    void findByAccountId_existingRecord_mapsToDomainModel() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        databaseService.record = new UserProfileRecord(1001L, "carry-user", "", "", BASE_TIME, BASE_TIME);
        StarterUserProfileRepository repository = new StarterUserProfileRepository(databaseService);

        Optional<UserProfile> result = repository.findByAccountId(1001L);

        assertTrue(result.isPresent());
        assertEquals(1001L, result.orElseThrow().accountId());
        assertEquals("carry-user", result.orElseThrow().nickname());
        assertEquals("", result.orElseThrow().avatarUrl());
        assertEquals("", result.orElseThrow().bio());
    }

    /**
     * 验证保存领域模型时会转换并写入 database-api 记录。
     */
    @Test
    @DisplayName("save domain model writes database record")
    void save_domainModel_writesDatabaseRecord() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        StarterUserProfileRepository repository = new StarterUserProfileRepository(databaseService);
        UserProfile userProfile = new UserProfile(1001L, "carry-user", "", "", BASE_TIME, BASE_TIME);

        UserProfile result = repository.save(userProfile);

        assertEquals(userProfile, result);
        assertEquals(1001L, databaseService.insertedRecord.accountId());
        assertEquals("carry-user", databaseService.insertedRecord.nickname());
        assertEquals("", databaseService.insertedRecord.avatarUrl());
        assertEquals("", databaseService.insertedRecord.bio());
    }

    /**
     * 验证更新领域模型时会转换并写入 database-api 记录。
     */
    @Test
    @DisplayName("update domain model writes database record")
    void update_domainModel_writesDatabaseRecord() {
        FakeUserProfileDatabaseService databaseService = new FakeUserProfileDatabaseService();
        StarterUserProfileRepository repository = new StarterUserProfileRepository(databaseService);
        UserProfile userProfile = new UserProfile(1001L, "new-name", "", "new bio", BASE_TIME, BASE_TIME);

        UserProfile result = repository.update(userProfile);

        assertEquals(userProfile, result);
        assertEquals(1001L, databaseService.updatedRecord.accountId());
        assertEquals("new-name", databaseService.updatedRecord.nickname());
        assertEquals("new bio", databaseService.updatedRecord.bio());
    }

    private static class FakeUserProfileDatabaseService implements UserProfileDatabaseService {

        private UserProfileRecord record;
        private UserProfileRecord insertedRecord;
        private UserProfileRecord updatedRecord;

        @Override
        public Optional<UserProfileRecord> findByAccountId(long accountId) {
            return Optional.ofNullable(record);
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
