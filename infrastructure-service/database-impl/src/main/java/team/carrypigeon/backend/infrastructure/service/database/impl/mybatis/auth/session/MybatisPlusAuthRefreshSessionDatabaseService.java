package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.session.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.session.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session.AuthRefreshSessionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session.AuthRefreshSessionMapper;

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

    /**
     * 按刷新会话标识读取当前会话记录。
     * 输入：刷新会话主键。
     * 输出：存在时返回数据库快照，不存在时返回空。
     *
     * @param sessionId 刷新会话标识
     * @return 刷新会话记录快照
     * @throws DatabaseServiceException 底层查询失败时抛出
     */
    @Override
    public Optional<AuthRefreshSessionRecord> findById(long sessionId) {
        return execute(
                () -> Optional.ofNullable(authRefreshSessionMapper.selectById(sessionId)).map(AuthRefreshSessionEntity::toRecord),
                "failed to query auth refresh session by id"
        );
    }

    /**
     * 写入新的刷新会话记录。
     * 输入：已完成业务校验的刷新会话快照。
     * 副作用：向刷新会话表插入一条记录。
     *
     * @param record 刷新会话记录
     * @throws DatabaseServiceException 底层写入失败时抛出
     */
    @Override
    public void insert(AuthRefreshSessionRecord record) {
        executeVoid(() -> authRefreshSessionMapper.insert(AuthRefreshSessionEntity.fromRecord(record)), "failed to insert auth refresh session");
    }

    /**
     * 撤销指定刷新会话。
     * 输入：刷新会话主键。
     * 副作用：将对应会话标记为 revoked。
     *
     * @param sessionId 刷新会话标识
     * @throws DatabaseServiceException 底层更新失败时抛出
     */
    @Override
    public void revoke(long sessionId) {
        executeVoid(() -> authRefreshSessionMapper.revokeById(sessionId), "failed to revoke auth refresh session");
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

    /**
     * 有返回值的数据库访问操作。
     * 职责：让统一异常包装方法接收 mapper 查询或写入返回值。
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    /**
     * 无返回值的数据库访问操作。
     * 职责：让统一异常包装方法复用同一条数据库异常转换路径。
     */
    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }

}
