package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.time.Clock;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/** AuthRefreshSession 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryAuthRefreshSessionService extends LongIdTable<AuthRefreshSessionRecord> implements AuthRefreshSessionDatabaseService {
    private final Clock clock;

    public MemoryAuthRefreshSessionService(MemoryDatabase database, Clock clock) {
        super(database, AuthRefreshSessionRecord::id);
        this.clock = clock;
    }

    @Override
    public void revoke(long sessionId) {
        database.locked(() -> {
            findById(sessionId).ifPresent(r -> update(revoked(r)));
            return null;
        });
    }

    @Override
    public boolean revokeIfActive(long sessionId) {
        return Boolean.TRUE.equals(database.locked(() -> {
            return findById(sessionId).filter(r -> !r.revoked() && r.expiresAt().isAfter(clock.instant()))
                .map(r -> update(revoked(r))).orElse(false);
        }));
    }

    private AuthRefreshSessionRecord revoked(AuthRefreshSessionRecord r) {
        return new AuthRefreshSessionRecord(r.id(), r.accountId(), r.refreshTokenHash(), r.expiresAt(),
                true, r.createdAt(), clock.instant());
    }
}
