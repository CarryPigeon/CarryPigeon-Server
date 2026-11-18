package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ChannelEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ChannelMapper;

/**
 * MyBatis-Plus 频道数据库服务。
 * 职责：在 database-impl 中完成频道的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载频道业务规则。
 * 缓存：按频道 ID 缓存单条与批量频道主数据；写路径按频道 ID 精确失效，不缓存分页/发现页查询。
 */
public class ChannelDatabaseServiceImpl implements ChannelDatabaseService {

    private static final String DOMAIN = "channel";

    private final ChannelMapper channelMapper;
    private final DatabaseCache databaseCache;

    /**
     * 创建频道数据库服务。
     *
     * @param channelMapper 频道 Mapper
     * @param databaseCache 数据库查询缓存门面
     */
    public ChannelDatabaseServiceImpl(ChannelMapper channelMapper, DatabaseCache databaseCache) {
        this.channelMapper = channelMapper;
        this.databaseCache = databaseCache;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void scheduleDeletion(long channelId, long operatorAccountId) {
        channelMapper.scheduleDeletion(channelId, operatorAccountId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> findDeletionOperator(long channelId) {
        return Optional.ofNullable(channelMapper.selectDeletionOperator(channelId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> findPendingDeletionForUpdate() {
        return Optional.ofNullable(channelMapper.selectPendingDeletionForUpdate());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void completeDeletion(long channelId) {
        channelMapper.completeDeletion(channelId);
    }

    /** 授权数据只从数据库读取，避免缓存失效延迟继续授予旧权限。 */
    @Override
    public Optional<ChannelRecord> findByIdDirect(long channelId) {
        return queryById(channelId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ChannelRecord> findByIdForUpdate(long channelId) {
        // 使用当前读：等待前一笔治理事务提交后再返回频道，锁由外层业务事务释放。
        return Optional.ofNullable(channelMapper.selectOne(new LambdaQueryWrapper<ChannelEntity>()
                        .eq(ChannelEntity::getChannelId, channelId)
                        .last("FOR UPDATE")))
                .map(ChannelEntity::toRecord);
    }

    /**
     * 批量插入频道记录。
     * 缓存：逐频道 ID 精确删除实体缓存。
     */
    @Override
    @Transactional
    public boolean insertAll(List<ChannelRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = channelMapper.insertAll(records.stream().map(ChannelEntity::from).toList());
        for (ChannelRecord record : records) {
            evictRecord(record.id());
        }
        return affectedRows == records.size();
    }
    /**
     * 查询全部 system 频道。
     * 边界：低频元能力查询，不属于当前缓存覆盖范围。
     */
    @Override
    public List<ChannelRecord> findSystemChannels() {
        return channelMapper.selectList(new LambdaQueryWrapper<ChannelEntity>()
                        .eq(ChannelEntity::getSystemChannel, true))
                .stream()
                .map(ChannelEntity::toRecord)
                .toList();
    }

    /**
     * 按频道 ID 查询频道。
     */
    @Override
    public Optional<ChannelRecord> findById(long channelId) {
        return databaseCache.load(
                DatabaseCache.entityKey(DOMAIN, channelId),
                ChannelRecord.class,
                () -> queryById(channelId));
    }

    /**
     * 批量读取频道。
     * 缓存：按频道 ID 批量读实体缓存，只对缺失主键回源数据库。
     */
    @Override
    public List<ChannelRecord> findByIds(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return List.of();
        }
        return databaseCache.loadAll(DOMAIN, channelIds, ChannelRecord.class, ChannelRecord::id, this::queryByIds);
    }

    /**
     * 按发现页规则查询频道集合。
     * 边界：带关键字与游标的低复用查询，不属于当前缓存覆盖范围。
     */
    @Override
    public List<ChannelDiscoverRecord> findPublicAfterId(String keyword, Long cursorChannelId, int limit) {
        // 参数绑定保证 SQL 结构，额外转义保证用户输入的 %、_ 和 ! 只按字面量参与包含匹配。
        String escapedKeyword = keyword == null ? null
                : keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return channelMapper.discoverChannels(escapedKeyword, cursorChannelId, limit);
    }

    /**
     * 插入新的频道记录。
     * 缓存：写入成功后按频道 ID 精确删除实体缓存。
     */
    @Override
    @Transactional
    public boolean insert(ChannelRecord record) {
        int affectedRows = channelMapper.insert(ChannelEntity.from(record));
        evictRecord(record.id());
        return affectedRows == 1;
    }

    /**
     * 更新既有频道记录。
     * 缓存：写入成功后按频道 ID 精确删除实体缓存。
     */
    @Override
    @Transactional
    public boolean update(ChannelRecord record) {
        int affectedRows = channelMapper.updateById(ChannelEntity.from(record));
        evictRecord(record.id());
        return affectedRows == 1;
    }

    /**
     * 分页查询频道记录。
     * 缓存：分页查询键空间无界，不参与缓存，保留给管理后台/离线维护使用。
     */
    @Override
    public PageResult<ChannelRecord> page(PageQuery query) {
        IPage<ChannelEntity> result = channelMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelEntity>()
                        .orderByAsc(ChannelEntity::getChannelId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按频道 ID 删除频道记录。
     * 缓存：删除成功后按频道 ID 精确删除实体缓存。
     */
    @Override
    @Transactional
    public boolean deleteById(long channelId) {
        int affectedRows = channelMapper.deleteById(channelId);
        evictRecord(channelId);
        return affectedRows == 1;
    }

    /**
     * 按频道 ID 批量删除记录。
     */
    @Override
    @Transactional
    public boolean deleteByIds(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return true;
        }
        int affectedRows = channelMapper.deleteByIds(channelIds);
        for (Long channelId : channelIds) {
            evictRecord(channelId);
        }
        return affectedRows > 0;
    }

    private Optional<ChannelRecord> queryById(long channelId) {
        return Optional.ofNullable(channelMapper.selectById(channelId))
                .map(ChannelEntity::toRecord);
    }

    private List<ChannelRecord> queryByIds(List<Long> channelIds) {
        return channelMapper.selectByIds(channelIds).stream()
                .map(ChannelEntity::toRecord)
                .toList();
    }

    private void evictRecord(long channelId) {
        databaseCache.evict(DatabaseCache.entityKey(DOMAIN, channelId));
    }
}
