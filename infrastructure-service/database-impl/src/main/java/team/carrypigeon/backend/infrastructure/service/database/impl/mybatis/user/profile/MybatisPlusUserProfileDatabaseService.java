package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile.UserProfileMapper;

/**
 * MyBatis-Plus 用户资料数据库服务。
 * 职责：在 database-impl 中完成用户资料的最小查询与更新。
 * 边界：只负责数据库记录映射，不承载用户资料业务规则与协议决策。
 */
public class MybatisPlusUserProfileDatabaseService implements UserProfileDatabaseService {

    private final UserProfileMapper userProfileMapper;

    public MybatisPlusUserProfileDatabaseService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * 按账户 ID 查询资料记录。
     */
    @Override
    public Optional<UserProfileRecord> findByAccountId(long accountId) {
        return execute(
                () -> Optional.ofNullable(userProfileMapper.selectById(accountId)).map(UserProfileEntity::toRecord),
                "failed to query user profile by account id"
        );
    }

    /**
     * 查询全部用户资料记录。
     * 输出：按账户 ID 升序返回稳定结果。
     */
    @Override
    public List<UserProfileRecord> findAll() {
        return execute(
                () -> userProfileMapper.selectList(new LambdaQueryWrapper<UserProfileEntity>()
                        .orderByAsc(UserProfileEntity::getAccountId)).stream()
                        .map(UserProfileEntity::toRecord)
                        .toList(),
                "failed to query user profiles"
        );
    }

    /**
     * 按账户游标倒序拉取资料记录。
     */
    @Override
    public List<UserProfileRecord> findByAccountIdBefore(Long cursorAccountId, int limit) {
        return execute(
                () -> userProfileMapper.selectList(new LambdaQueryWrapper<UserProfileEntity>()
                        .lt(cursorAccountId != null, UserProfileEntity::getAccountId, cursorAccountId)
                        .orderByDesc(UserProfileEntity::getAccountId)
                        .last("LIMIT " + limit)).stream()
                        .map(UserProfileEntity::toRecord)
                        .toList(),
                "failed to query user profiles by cursor"
        );
    }

    /**
     * 按昵称或简介关键字搜索资料记录。
     */
    @Override
    public List<UserProfileRecord> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return execute(
                () -> userProfileMapper.selectList(new LambdaQueryWrapper<UserProfileEntity>()
                        .lt(cursorAccountId != null, UserProfileEntity::getAccountId, cursorAccountId)
                        .and(wrapper -> wrapper.like(UserProfileEntity::getNickname, normalizedKeyword)
                                .or()
                                .like(UserProfileEntity::getBio, normalizedKeyword))
                        .orderByDesc(UserProfileEntity::getAccountId)
                        .last("LIMIT " + limit)).stream()
                        .map(UserProfileEntity::toRecord)
                        .toList(),
                "failed to search user profiles"
        );
    }

    /**
     * 插入新的用户资料记录。
     */
    @Override
    public void insert(UserProfileRecord record) {
        executeVoid(() -> userProfileMapper.insert(UserProfileEntity.fromRecord(record)), "failed to insert user profile");
    }

    /**
     * 更新既有用户资料记录。
     * 约束：若没有任何记录被更新，视为异常而不是静默成功。
     */
    @Override
    public void update(UserProfileRecord record) {
        executeVoid(() -> {
            int updatedRows = userProfileMapper.updateById(UserProfileEntity.fromRecord(record));
            if (updatedRows == 0) {
                throw new DatabaseServiceException("user profile update affected no rows");
            }
        }, "failed to update user profile");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        } catch (RuntimeException exception) {
            if (exception instanceof DatabaseServiceException databaseServiceException) {
                throw databaseServiceException;
            }
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
