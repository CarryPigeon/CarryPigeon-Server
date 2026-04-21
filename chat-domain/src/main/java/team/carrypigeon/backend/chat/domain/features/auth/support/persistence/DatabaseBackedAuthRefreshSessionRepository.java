package team.carrypigeon.backend.chat.domain.features.auth.support.persistence;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/**
 * 基于 database-api 的刷新会话仓储适配器。
 * 职责：在 auth feature 内完成 refresh session 领域模型与 database-api 契约模型之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，具体持久化由 database-impl 提供。
 */
public class DatabaseBackedAuthRefreshSessionRepository implements AuthRefreshSessionRepository {

    private final AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService;

    public DatabaseBackedAuthRefreshSessionRepository(
            AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService
    ) {
        this.authRefreshSessionDatabaseService = authRefreshSessionDatabaseService;
    }

    @Override
    public Optional<AuthRefreshSession> findById(long sessionId) {
        return authRefreshSessionDatabaseService.findById(sessionId)
                .map(this::toDomainModel);
    }

    @Override
    public AuthRefreshSession save(AuthRefreshSession session) {
        authRefreshSessionDatabaseService.insert(toRecord(session));
        return session;
    }

    @Override
    public void revoke(long sessionId) {
        authRefreshSessionDatabaseService.revoke(sessionId);
    }

    private AuthRefreshSession toDomainModel(AuthRefreshSessionRecord record) {
        return new AuthRefreshSession(
                record.id(),
                record.accountId(),
                record.refreshTokenHash(),
                record.expiresAt(),
                record.revoked(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private AuthRefreshSessionRecord toRecord(AuthRefreshSession session) {
        return new AuthRefreshSessionRecord(
                session.id(),
                session.accountId(),
                session.refreshTokenHash(),
                session.expiresAt(),
                session.revoked(),
                session.createdAt(),
                session.updatedAt()
        );
    }
}
