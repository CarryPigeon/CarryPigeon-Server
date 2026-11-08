package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;

/**
 * MyBatis-Plus 频道封禁数据库服务。
 * 职责：在 database-impl 中完成频道封禁的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载封禁业务规则。
 * 缓存：按 (频道, 被封禁账户) 唯一键缓存“关系 -&gt; 封禁主键”lookup 指针，封禁实体按主键缓存一份；
 * 写路径精确失效实体键与关系指针，封禁列表/分页查询不缓存。
 */
@Component
public class MybatisPlusChannelBanDatabaseService implements ChannelBanDatabaseService {

    private static final String DOMAIN = DatabaseCache.DOMAIN_CHANNEL_BAN;
    private static final String LOOKUP_RELATION = "relation";

    private final ChannelBanMapper channelBanMapper;
    private final DatabaseCache databaseCache;

    /**
     * 创建频道封禁数据库服务。
     *
     * @param channelBanMapper 频道封禁 Mapper
     * @param databaseCache 数据库查询缓存门面
     */
    public MybatisPlusChannelBanDatabaseService(ChannelBanMapper channelBanMapper, DatabaseCache databaseCache) {
        this.channelBanMapper = channelBanMapper;
        this.databaseCache = databaseCache;
    }

    /**
     * 查询频道对目标账户的封禁记录。
     * 缓存：先查 relation lookup 指针，命中后按封禁主键读实体缓存；未命中时一次数据库查询同时回填指针与实体。
     * 频道上下文校验会在请求主链路上高频读取封禁关系，这是本次新增缓存的目标查询。
     */
    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        String lookupKey = relationKey(channelId, bannedAccountId);
        Optional<ChannelBanRecord> ban = databaseCache.loadByLookup(
                lookupKey,
                DOMAIN,
                ChannelBanRecord.class,
                ChannelBanRecord::banId,
                () -> loadByRelation(channelId, bannedAccountId),
                this::loadById);
        // 指针指向的封禁记录与请求关系不一致时，清理过期指针并回退数据库权威查询。
        if (ban.isPresent() && (ban.get().channelId() != channelId || ban.get().bannedAccountId() != bannedAccountId)) {
            databaseCache.evict(lookupKey);
            return loadByRelation(channelId, bannedAccountId);
        }
        return ban;
    }

    /**
     * 分页查询频道下的封禁记录。
     * 边界：分页键空间无界且复用低，不属于当前缓存覆盖范围。
     */
    @Override
    public PageResult<ChannelBanRecord> pageBansByChannelId(long channelId, PageQuery query) {
        IPage<ChannelBanEntity> result = channelBanMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId)
                        .orderByDesc(ChannelBanEntity::getBanId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelBanEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 插入新的封禁记录。
     */
    @Override
    public void insert(ChannelBanRecord record) {
        channelBanMapper.insert(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
    }

    /**
     * 批量插入封禁记录。
     */
    @Override
    public void insertAll(List<ChannelBanRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        channelBanMapper.insertAll(records.stream().map(ChannelBanEntity::from).toList());
        for (ChannelBanRecord record : records) {
            evictRecord(record.banId());
            evictRelation(record.channelId(), record.bannedAccountId());
        }
    }

    /**
     * 按封禁 ID 删除记录。
     * 缓存：删除前读取旧记录，删除成功后精确失效实体键与关系指针。
     */
    @Override
    public void delete(long banId) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled() ? loadById(banId) : Optional.empty();
        channelBanMapper.deleteById(banId);
        evictRecord(banId);
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
    }

    /**
     * 按封禁 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return;
        }
        // 删除前读取旧记录，删除成功后逐条精确失效实体与关系指针。
        List<ChannelBanRecord> previous = databaseCache.enabled() ? loadByIds(banIds) : List.of();
        channelBanMapper.deleteByIds(banIds);
        for (Long banId : banIds) {
            evictRecord(banId);
        }
        for (ChannelBanRecord record : previous) {
            evictRelation(record.channelId(), record.bannedAccountId());
        }
    }

    /**
     * 按封禁 ID 查询记录。
     */
    @Override
    public Optional<ChannelBanRecord> findById(long banId) {
        return loadById(banId);
    }

    /**
     * 按封禁 ID 批量查询记录。
     */
    @Override
    public List<ChannelBanRecord> findByIds(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return List.of();
        }
        return loadByIds(banIds);
    }

    /**
     * 更新既有封禁记录。
     * 缓存：更新前读取旧记录，写入成功后精确失效新实体、旧/新关系指针。
     */
    @Override
    public void update(ChannelBanRecord record) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled()
                ? loadById(record.banId())
                : Optional.empty();
        channelBanMapper.updateById(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
    }

    /**
     * 分页查询封禁记录。
     * 缓存：分页查询键空间无界，不参与缓存，保留给管理后台/离线维护使用。
     */
    @Override
    public PageResult<ChannelBanRecord> list(PageQuery query) {
        IPage<ChannelBanEntity> result = channelBanMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelBanEntity>()
                        .orderByAsc(ChannelBanEntity::getBanId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelBanEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 删除频道下的全部封禁记录。
     * 缓存：缓存启用时先读取该频道封禁快照，删除成功后逐条精确失效实体与关系指针。
     */
    @Override
    public void deleteByChannelId(long channelId) {
        List<ChannelBanRecord> previous = databaseCache.enabled()
                ? loadByChannelId(channelId)
                : List.of();
        channelBanMapper.delete(new LambdaQueryWrapper<ChannelBanEntity>()
                .eq(ChannelBanEntity::getChannelId, channelId));
        for (ChannelBanRecord record : previous) {
            evictRecord(record.banId());
            evictRelation(record.channelId(), record.bannedAccountId());
        }
    }

    private Optional<ChannelBanRecord> loadById(long banId) {
        return Optional.ofNullable(channelBanMapper.selectById(banId))
                .map(ChannelBanEntity::toRecord);
    }

    private List<ChannelBanRecord> loadByIds(List<Long> banIds) {
        return channelBanMapper.selectByIds(banIds).stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    private Optional<ChannelBanRecord> loadByRelation(long channelId, long bannedAccountId) {
        return Optional.ofNullable(channelBanMapper.selectOne(new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId)
                        .eq(ChannelBanEntity::getBannedAccountId, bannedAccountId)))
                .map(ChannelBanEntity::toRecord);
    }

    private List<ChannelBanRecord> loadByChannelId(long channelId) {
        return channelBanMapper.selectList(new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId))
                .stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    private String relationKey(long channelId, long bannedAccountId) {
        return DatabaseCache.lookupKey(
                DOMAIN, LOOKUP_RELATION, Long.toString(channelId), Long.toString(bannedAccountId));
    }

    private void evictRecord(long banId) {
        databaseCache.evict(DatabaseCache.entityKey(DOMAIN, banId));
    }

    private void evictRelation(long channelId, long bannedAccountId) {
        databaseCache.evict(relationKey(channelId, bannedAccountId));
    }
}
