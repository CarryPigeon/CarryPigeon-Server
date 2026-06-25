package team.carrypigeon.backend.infrastructure.service.database.api.auth.account;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthAccountDatabaseService 行为契约测试。
 * 职责：锁定鉴权账户数据库抽象在查询、插入与更新场景下的最小稳定语义。
 * 边界：只验证接口文档层面的读写契约，不依赖具体数据库实现。
 */
@Tag("contract")
class AuthAccountDatabaseServiceContractTests {

    private static final AuthAccountRecord RECORD = new AuthAccountRecord(
            1001L,
            "carry-user",
            "hashed-password",
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-21T12:00:00Z")
    );

    /**
     * 验证已写入账户可通过用户名查询命中同一记录快照。
     */
    @Test
    @DisplayName("find by username inserted record returns account snapshot")
    void findByUsername_insertedRecord_returnsAccountSnapshot() {
        InMemoryAuthAccountDatabaseService service = new InMemoryAuthAccountDatabaseService();
        service.insert(RECORD);

        Optional<AuthAccountRecord> result = service.findByUsername("carry-user");

        assertEquals(Optional.of(RECORD), result);
    }

    /**
     * 验证未写入账户时按账户 ID 查询会返回空。
     */
    @Test
    @DisplayName("find by id missing record returns empty optional")
    void findById_missingRecord_returnsEmptyOptional() {
        AuthAccountDatabaseService service = new InMemoryAuthAccountDatabaseService();

        Optional<AuthAccountRecord> result = service.findById(1001L);

        assertTrue(result.isEmpty());
    }

    /**
     * 验证更新后后续查询会观察到新的账户快照。
     */
    @Test
    @DisplayName("update existing record replaces stored snapshot")
    void update_existingRecord_replacesStoredSnapshot() {
        InMemoryAuthAccountDatabaseService service = new InMemoryAuthAccountDatabaseService();
        service.insert(RECORD);
        AuthAccountRecord updatedRecord = new AuthAccountRecord(
                1001L,
                "carry-user",
                "hashed-password-v2",
                RECORD.createdAt(),
                Instant.parse("2026-04-22T12:00:00Z")
        );

        service.update(updatedRecord);

        assertEquals(Optional.of(updatedRecord), service.findById(1001L));
        assertEquals(Optional.of(updatedRecord), service.findByUsername("carry-user"));
    }

    private static final class InMemoryAuthAccountDatabaseService implements AuthAccountDatabaseService {

        private final Map<Long, AuthAccountRecord> recordsById = new HashMap<>();
        private final Map<String, Long> accountIdsByUsername = new HashMap<>();

        @Override
        public Optional<AuthAccountRecord> findByUsername(String username) {
            Long accountId = accountIdsByUsername.get(username);
            return accountId == null ? Optional.empty() : findById(accountId);
        }

        @Override
        public Optional<AuthAccountRecord> findById(long accountId) {
            return Optional.ofNullable(recordsById.get(accountId));
        }

        @Override
        public void insert(AuthAccountRecord record) {
            recordsById.put(record.id(), record);
            accountIdsByUsername.put(record.username(), record.id());
        }

        @Override
        public void update(AuthAccountRecord record) {
            insert(record);
        }
    }
}
