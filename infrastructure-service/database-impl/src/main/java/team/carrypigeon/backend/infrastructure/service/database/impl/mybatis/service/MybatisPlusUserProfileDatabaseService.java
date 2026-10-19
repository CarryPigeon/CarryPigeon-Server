package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;

/**
 * MyBatis-Plus 用户资料数据库服务。
 * 职责：在 database-impl 中完成用户资料的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载资料校验与展示业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusUserProfileDatabaseService implements UserProfileDatabaseService {

    private final UserProfileMapper userProfileMapper;

    public MybatisPlusUserProfileDatabaseService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * 按账户 ID 查询用户资料。
     */
    @Override
    public Optional<UserProfileRecord> findByAccountId(long accountId) {
        return Optional.ofNullable(userProfileMapper.selectById(accountId))
                .map(UserProfileEntity::toRecord);
    }

    /**
     * 批量读取用户资料。
     */
    @Override
    public List<UserProfileRecord> findByIds(Collection<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return userProfileMapper.selectByIds(accountIds).stream()
                .map(UserProfileEntity::toRecord)
                .toList();
    }

    /**
     * 插入新的用户资料记录。
     */
    @Override
    public void insert(UserProfileRecord record) {
        userProfileMapper.insert(UserProfileEntity.from(record));
    }

    /**
     * 更新既有用户资料记录。
     */
    @Override
    public void update(UserProfileRecord record) {
        userProfileMapper.updateById(UserProfileEntity.from(record));
    }
}
