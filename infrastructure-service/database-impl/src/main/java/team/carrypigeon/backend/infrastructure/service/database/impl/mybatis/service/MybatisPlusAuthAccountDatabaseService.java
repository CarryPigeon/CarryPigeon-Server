package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthAccountEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;

/**
 * MyBatis-Plus 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入。
 * 边界：只负责数据库记录映射，不承载注册业务规则与协议决策。
 */
public class MybatisPlusAuthAccountDatabaseService implements AuthAccountDatabaseService {

    private static final String LIMIT_ONE = "LIMIT 1";

    private final AuthAccountMapper authAccountMapper;

    public MybatisPlusAuthAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        this.authAccountMapper = authAccountMapper;
    }

    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        return execute(() -> {
            AuthAccountEntity entity = authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                    .eq(AuthAccountEntity::getUsername, username)
                    .last(LIMIT_ONE));
            return Optional.ofNullable(entity).map(this::toRecord);
        }, "failed to query auth account by username");
    }

    @Override
    public void insert(AuthAccountRecord record) {
        executeVoid(() -> authAccountMapper.insert(toEntity(record)), "failed to insert auth account");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }

    private AuthAccountRecord toRecord(AuthAccountEntity entity) {
        return new AuthAccountRecord(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AuthAccountEntity toEntity(AuthAccountRecord record) {
        AuthAccountEntity entity = new AuthAccountEntity();
        entity.setId(record.id());
        entity.setUsername(record.username());
        entity.setPasswordHash(record.passwordHash());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }
}
