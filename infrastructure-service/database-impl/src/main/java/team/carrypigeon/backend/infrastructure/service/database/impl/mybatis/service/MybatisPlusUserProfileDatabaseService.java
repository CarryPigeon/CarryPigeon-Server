package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

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
 * 职责：在 database-impl 中完成用户资料的通用元能力。
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
     * 插入新的用户资料记录。
     */
    @Override
    public void insert(UserProfileRecord record) {
        userProfileMapper.insert(UserProfileEntity.from(record));
    }

    /**
     * 批量插入用户资料记录。
     */
    @Override
    public void insertAll(List<UserProfileRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        userProfileMapper.insertAll(records.stream().map(UserProfileEntity::from).toList());
    }

    /**
     * 按账户 ID 删除记录。
     */
    @Override
    public void delete(long accountId) {
        userProfileMapper.deleteById(accountId);
    }

    /**
     * 按账户 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return;
        }
        userProfileMapper.deleteByIds(accountIds);
    }

    /**
     * 更新既有用户资料记录。
     */
    @Override
    public void update(UserProfileRecord record) {
        userProfileMapper.updateById(UserProfileEntity.from(record));
    }

    /**
     * 查询用户资料表全部记录。
     */
    @Override
    public List<UserProfileRecord> list() {
        return userProfileMapper.selectList(null).stream()
                .map(UserProfileEntity::toRecord)
                .toList();
    }

    /**
     * 按账户 ID 查询用户资料。
     */
    @Override
    public Optional<UserProfileRecord> findById(long accountId) {
        return Optional.ofNullable(userProfileMapper.selectById(accountId))
                .map(UserProfileEntity::toRecord);
    }

    /**
     * 批量读取用户资料。
     */
    @Override
    public List<UserProfileRecord> findByIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return userProfileMapper.selectByIds(accountIds).stream()
                .map(UserProfileEntity::toRecord)
                .toList();
    }
}
