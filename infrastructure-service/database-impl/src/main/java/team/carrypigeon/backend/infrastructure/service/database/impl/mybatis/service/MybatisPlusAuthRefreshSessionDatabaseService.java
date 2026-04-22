package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;

/**
 * MyBatis-Plus 刷新会话数据库服务。
 * 职责：在 database-impl 中完成 refresh session 的最小查询、写入与撤销。
 * 边界：只负责数据库记录映射，不承载 token 业务规则。
 */
public class MybatisPlusAuthRefreshSessionDatabaseService implements AuthRefreshSessionDatabaseService {

    private final AuthRefreshSessionMapper authRefreshSessionMapper;

    public MybatisPlusAuthRefreshSessionDatabaseService(AuthRefreshSessionMapper authRefreshSessionMapper) {
        this.authRefreshSessionMapper = authRefreshSessionMapper;
    }

    @Override
    public Optional<AuthRefreshSessionRecord> findById(long sessionId) {
        try {
            return Optional.ofNullable(authRefreshSessionMapper.selectById(sessionId)).map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query auth refresh session by id", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query auth refresh session by id", exception);
        }
    }

    @Override
    public void insert(AuthRefreshSessionRecord record) {
        try {
            authRefreshSessionMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert auth refresh session", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert auth refresh session", exception);
        }
    }

    @Override
    public void revoke(long sessionId) {
        try {
            authRefreshSessionMapper.revokeById(sessionId);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to revoke auth refresh session", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to revoke auth refresh session", exception);
        }
    }

    private AuthRefreshSessionRecord toRecord(AuthRefreshSessionEntity entity) {
        return new AuthRefreshSessionRecord(
                entity.getId(),
                entity.getAccountId(),
                entity.getRefreshTokenHash(),
                entity.getExpiresAt(),
                Boolean.TRUE.equals(entity.getRevoked()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AuthRefreshSessionEntity toEntity(AuthRefreshSessionRecord record) {
        AuthRefreshSessionEntity entity = new AuthRefreshSessionEntity();
        entity.setId(record.id());
        entity.setAccountId(record.accountId());
        entity.setRefreshTokenHash(record.refreshTokenHash());
        entity.setExpiresAt(record.expiresAt());
        entity.setRevoked(record.revoked());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}
