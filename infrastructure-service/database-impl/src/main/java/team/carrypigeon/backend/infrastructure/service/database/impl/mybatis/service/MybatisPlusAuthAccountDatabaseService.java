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
 * TODO 可以将service、model和entity进行重整理，一个service及其对应的model放入一个包中，例如service/AuthAccount/{databaseservice和models}（针对当前包下的所有内容）
 * TODO toRecord和toEntity应该定义在entity中而不是service下，应该保持职责的单一纯净
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

    /**
     * 按用户名查询鉴权账户记录。
     */
    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        return execute(() -> {
            AuthAccountEntity entity = authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                    .eq(AuthAccountEntity::getUsername, username)
                    .last(LIMIT_ONE));
            return Optional.ofNullable(entity).map(this::toRecord);
        }, "failed to query auth account by username");
    }

    /**
     * 按账户 ID 查询鉴权账户记录。
     */
    @Override
    public Optional<AuthAccountRecord> findById(long accountId) {
        return execute(() -> Optional.ofNullable(authAccountMapper.selectById(accountId)).map(this::toRecord),
                "failed to query auth account by id");
    }

    /**
     * 插入新的鉴权账户记录。
     */
    @Override
    public void insert(AuthAccountRecord record) {
        executeVoid(() -> authAccountMapper.insert(toEntity(record)), "failed to insert auth account");
    }

    /**
     * 更新既有鉴权账户记录。
     */
    @Override
    public void update(AuthAccountRecord record) {
        executeVoid(() -> authAccountMapper.updateById(toEntity(record)), "failed to update auth account");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
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
