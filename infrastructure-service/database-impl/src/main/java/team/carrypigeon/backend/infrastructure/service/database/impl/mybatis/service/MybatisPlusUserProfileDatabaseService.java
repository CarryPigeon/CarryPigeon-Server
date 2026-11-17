package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;

/**
 * MyBatis-Plus 用户资料数据库服务。
 * 职责：在 database-impl 中完成用户资料的通用元能力。
 * 边界：只负责数据库记录映射，不承载资料校验与展示业务规则。
 * 缓存：按账户 ID 缓存单条与批量资料读取；写路径按账户 ID 精确失效，不缓存分页查询。
 */
public class MybatisPlusUserProfileDatabaseService implements UserProfileDatabaseService {

    private static final String DOMAIN = DatabaseCache.DOMAIN_USER_PROFILE;

    private final UserProfileMapper userProfileMapper;
    private final DatabaseCache databaseCache;

    /**
     * 创建用户资料数据库服务。
     *
     * @param userProfileMapper 用户资料 Mapper
     * @param databaseCache 数据库查询缓存门面
     */
    public MybatisPlusUserProfileDatabaseService(UserProfileMapper userProfileMapper, DatabaseCache databaseCache) {
        this.userProfileMapper = userProfileMapper;
        this.databaseCache = databaseCache;
    }

    /**
     * 插入新的用户资料记录。
     * 缓存：写入成功后按账户 ID 精确删除实体缓存。
     */
    @Override
    public void insert(UserProfileRecord record) {
        userProfileMapper.insert(UserProfileEntity.from(record));
        evictRecord(record.accountId());
    }

    /**
     * 批量插入用户资料记录。
     * 缓存：逐账户 ID 精确删除实体缓存。
     */
    @Override
    public void insertAll(List<UserProfileRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        userProfileMapper.insertAll(records.stream().map(UserProfileEntity::from).toList());
        for (UserProfileRecord record : records) {
            evictRecord(record.accountId());
        }
    }

    /**
     * 按账户 ID 删除记录。
     * 缓存：删除成功后按账户 ID 精确删除实体缓存。
     */
    @Override
    public void delete(long accountId) {
        userProfileMapper.deleteById(accountId);
        evictRecord(accountId);
    }

    /**
     * 按账户 ID 批量删除记录。
     * 缓存：逐账户 ID 精确删除实体缓存。
     */
    @Override
    public void deleteAll(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return;
        }
        userProfileMapper.deleteByIds(accountIds);
        for (Long accountId : accountIds) {
            evictRecord(accountId);
        }
    }

    /**
     * 更新既有用户资料记录。
     * 缓存：写入成功后按账户 ID 精确删除实体缓存。
     */
    @Override
    public void update(UserProfileRecord record) {
        userProfileMapper.updateById(UserProfileEntity.from(record));
        evictRecord(record.accountId());
    }

    /**
     * 分页查询用户资料记录。
     * 缓存：分页查询键空间无界，不参与缓存，保留给管理后台/离线维护使用。
     */
    @Override
    public PageResult<UserProfileRecord> list(PageQuery query) {
        IPage<UserProfileEntity> result = userProfileMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<UserProfileEntity>()
                        .orderByAsc(UserProfileEntity::getAccountId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(UserProfileEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按账户 ID 查询用户资料。
     */
    @Override
    public Optional<UserProfileRecord> findById(long accountId) {
        return databaseCache.load(
                DatabaseCache.entityKey(DOMAIN, accountId),
                UserProfileRecord.class,
                () -> loadById(accountId));
    }

    /**
     * 批量读取用户资料。
     * 缓存：按账户 ID 批量读实体缓存，只对缺失主键回源数据库。
     */
    @Override
    public List<UserProfileRecord> findByIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return databaseCache.loadAll(DOMAIN, accountIds, UserProfileRecord.class, UserProfileRecord::accountId, this::loadByIds);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UserProfileRecord> findByIdForUpdate(long accountId) {
        return Optional.ofNullable(userProfileMapper.selectByAccountForUpdate(accountId)).map(UserProfileEntity::toRecord);
    }

    private Optional<UserProfileRecord> loadById(long accountId) {
        return Optional.ofNullable(userProfileMapper.selectById(accountId))
                .map(UserProfileEntity::toRecord);
    }

    private List<UserProfileRecord> loadByIds(List<Long> accountIds) {
        return userProfileMapper.selectByIds(accountIds).stream()
                .map(UserProfileEntity::toRecord)
                .toList();
    }

    private void evictRecord(long accountId) {
        databaseCache.evict(DatabaseCache.entityKey(DOMAIN, accountId));
    }
}
