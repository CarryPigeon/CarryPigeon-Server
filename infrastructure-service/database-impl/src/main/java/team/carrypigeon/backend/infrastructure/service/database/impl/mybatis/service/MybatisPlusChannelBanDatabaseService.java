package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;

/** 频道禁言持久化适配；提供权威权限查询、展示缓存与限量清理，不决定禁言业务规则。 */
public class MybatisPlusChannelBanDatabaseService implements ChannelBanDatabaseService {

    private static final String DOMAIN = DatabaseCache.DOMAIN_CHANNEL_BAN;
    private static final String LOOKUP_RELATION = "relation";

    private final ChannelBanMapper channelBanMapper;
    private final DatabaseCache databaseCache;

    public MybatisPlusChannelBanDatabaseService(ChannelBanMapper channelBanMapper, DatabaseCache databaseCache) {
        this.channelBanMapper = channelBanMapper;
        this.databaseCache = databaseCache;
    }

    /** 在外层清理事务中限量删除禁言记录，失败时随本批一起回滚。 */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ChannelBanRecord> deleteBatchByChannelId(long channelId, int limit) {
        List<ChannelBanRecord> records = channelBanMapper.selectList(new LambdaQueryWrapper<ChannelBanEntity>()
                .eq(ChannelBanEntity::getChannelId, channelId)
                .orderByAsc(ChannelBanEntity::getBanId)
                .last("LIMIT " + limit))
                .stream().map(ChannelBanEntity::toRecord).toList();
        deleteAll(records.stream().map(ChannelBanRecord::banId).toList());
        return records;
    }

    /** 授权数据只从数据库读取，避免缓存失效延迟继续授予旧权限。 */
    @Override
    public Optional<ChannelBanRecord> findByRelationForAuthorization(long channelId, long bannedAccountId) {
        return loadByRelation(channelId, bannedAccountId);
    }

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

    @Override
    @Transactional
    public void insert(ChannelBanRecord record) {
        channelBanMapper.insert(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
    }

    @Override
    @Transactional
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

    @Override
    @Transactional
    public void delete(long banId) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled() ? loadById(banId) : Optional.empty();
        channelBanMapper.deleteById(banId);
        evictRecord(banId);
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
    }

    @Override
    @Transactional
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

    @Override
    public Optional<ChannelBanRecord> findById(long banId) {
        return loadById(banId);
    }

    @Override
    public List<ChannelBanRecord> findByIds(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return List.of();
        }
        return loadByIds(banIds);
    }

    @Override
    @Transactional
    public void update(ChannelBanRecord record) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled()
                ? loadById(record.banId())
                : Optional.empty();
        channelBanMapper.updateById(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
    }

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

    @Override
    @Transactional
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
