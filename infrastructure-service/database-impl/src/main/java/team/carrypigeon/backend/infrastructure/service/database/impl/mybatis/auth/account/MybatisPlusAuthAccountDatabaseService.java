package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.account.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.account.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.AuthAccountEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.AuthAccountMapper;

/**
 * MyBatis-Plus 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入，并保持 service 层为薄适配器。
 * 边界：不承载注册业务规则与协议决策，记录映射逻辑下沉到对应实体。
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
            return Optional.ofNullable(entity).map(AuthAccountEntity::toRecord);
        }, "failed to query auth account by username");
    }

    /**
     * 按账户 ID 查询鉴权账户记录。
     */
    @Override
    public Optional<AuthAccountRecord> findById(long accountId) {
        return execute(() -> Optional.ofNullable(authAccountMapper.selectById(accountId)).map(AuthAccountEntity::toRecord),
                "failed to query auth account by id");
    }

    /**
     * 插入新的鉴权账户记录。
     */
    @Override
    public void insert(AuthAccountRecord record) {
        executeVoid(() -> authAccountMapper.insert(AuthAccountEntity.fromRecord(record)), "failed to insert auth account");
    }

    /**
     * 更新既有鉴权账户记录。
     */
    @Override
    public void update(AuthAccountRecord record) {
        executeVoid(() -> authAccountMapper.updateById(AuthAccountEntity.fromRecord(record)), "failed to update auth account");
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

    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }
}
