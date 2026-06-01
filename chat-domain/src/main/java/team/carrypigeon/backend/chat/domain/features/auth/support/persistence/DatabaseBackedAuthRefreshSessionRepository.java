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

    /**
     * 按主键查询刷新会话。
     * 输入：刷新会话标识。
     * 输出：存在时返回领域会话快照。
     *
     * @param sessionId 刷新会话标识
     * @return 领域刷新会话快照
     */
    @Override
    public Optional<AuthRefreshSession> findById(long sessionId) {
        return authRefreshSessionDatabaseService.findById(sessionId)
                .map(this::toDomainModel);
    }

    /**
     * 保存刷新会话快照。
     * 输入：领域层已构造完成的刷新会话。
     * 副作用：向数据库写入一条刷新会话记录。
     *
     * @param session 刷新会话领域对象
     * @return 原样返回保存后的领域对象
     */
    @Override
    public AuthRefreshSession save(AuthRefreshSession session) {
        authRefreshSessionDatabaseService.insert(toWriteRecord(session));
        return session;
    }

    /**
     * 撤销指定刷新会话。
     * 输入：刷新会话标识。
     * 副作用：委托 database-api 将会话标记为 revoked。
     *
     * @param sessionId 刷新会话标识
     */
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

    private AuthRefreshSessionRecord toWriteRecord(AuthRefreshSession session) {
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
