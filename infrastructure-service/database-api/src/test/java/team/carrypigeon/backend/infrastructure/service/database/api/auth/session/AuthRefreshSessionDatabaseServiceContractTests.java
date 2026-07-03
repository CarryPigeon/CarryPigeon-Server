package team.carrypigeon.backend.infrastructure.service.database.api.auth.session;

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
 * AuthRefreshSessionDatabaseService 行为契约测试。
 * 职责：锁定刷新会话数据库抽象在查询、写入与撤销场景下的最小稳定语义。
 * 边界：只验证接口文档层面的记录快照契约，不依赖具体数据库实现。
 */
@Tag("contract")
class AuthRefreshSessionDatabaseServiceContractTests {

    private static final AuthRefreshSessionRecord RECORD = new AuthRefreshSessionRecord(
            9001L,
            1001L,
            "refresh-hash",
            Instant.parse("2026-05-01T12:00:00Z"),
            false,
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-21T12:00:00Z")
    );

    /**
     * 验证写入会话后按 ID 查询会返回同一快照。
     */
    @Test
    @DisplayName("find by id inserted session returns snapshot")
    void findById_insertedSession_returnsSnapshot() {
        InMemoryAuthRefreshSessionDatabaseService service = new InMemoryAuthRefreshSessionDatabaseService();
        service.insert(RECORD);

        Optional<AuthRefreshSessionRecord> result = service.findById(9001L);

        assertEquals(Optional.of(RECORD), result);
    }

    /**
     * 验证未写入会话时查询返回空。
     */
    @Test
    @DisplayName("find by id missing session returns empty optional")
    void findById_missingSession_returnsEmptyOptional() {
        AuthRefreshSessionDatabaseService service = new InMemoryAuthRefreshSessionDatabaseService();

        Optional<AuthRefreshSessionRecord> result = service.findById(9001L);

        assertTrue(result.isEmpty());
    }

    /**
     * 验证撤销会话后后续读取会观察到 revoked 状态。
     */
    @Test
    @DisplayName("revoke existing session marks snapshot revoked")
    void revoke_existingSession_marksSnapshotRevoked() {
        InMemoryAuthRefreshSessionDatabaseService service = new InMemoryAuthRefreshSessionDatabaseService();
        service.insert(RECORD);

        service.revoke(9001L);

        assertEquals(Optional.of(new AuthRefreshSessionRecord(
                9001L,
                1001L,
                "refresh-hash",
                Instant.parse("2026-05-01T12:00:00Z"),
                true,
                Instant.parse("2026-04-20T12:00:00Z"),
                Instant.parse("2026-04-21T12:00:00Z")
        )), service.findById(9001L));
    }

    /**
     * `InMemoryAuthRefreshSessionDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class InMemoryAuthRefreshSessionDatabaseService implements AuthRefreshSessionDatabaseService {

        private final Map<Long, AuthRefreshSessionRecord> recordsById = new HashMap<>();

        @Override
        public Optional<AuthRefreshSessionRecord> findById(long sessionId) {
            return Optional.ofNullable(recordsById.get(sessionId));
        }

        @Override
        public void insert(AuthRefreshSessionRecord record) {
            recordsById.put(record.id(), record);
        }

        @Override
        public void revoke(long sessionId) {
            AuthRefreshSessionRecord record = recordsById.get(sessionId);
            if (record == null) {
                return;
            }
            recordsById.put(sessionId, new AuthRefreshSessionRecord(
                    record.id(),
                    record.accountId(),
                    record.refreshTokenHash(),
                    record.expiresAt(),
                    true,
                    record.createdAt(),
                    record.updatedAt()
            ));
        }
    }
}
