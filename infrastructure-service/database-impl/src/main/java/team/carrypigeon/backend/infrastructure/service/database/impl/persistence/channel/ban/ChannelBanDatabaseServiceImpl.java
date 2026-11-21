package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ban;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;

/** 频道禁言持久化适配；提供权威权限查询、展示缓存与限量清理，不决定禁言业务规则。 */
public class ChannelBanDatabaseServiceImpl implements ChannelBanDatabaseService {

    private static final String DOMAIN = "channel-ban";
    private static final String LOOKUP_RELATION = "relation";

    private final ChannelBanMapper channelBanMapper;
    private final DatabaseCache databaseCache;

    public ChannelBanDatabaseServiceImpl(ChannelBanMapper channelBanMapper, DatabaseCache databaseCache) {
        this.channelBanMapper = channelBanMapper;
        this.databaseCache = databaseCache;
    }

    /** 授权数据只从数据库读取，避免缓存失效延迟继续授予旧权限。 */
    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountIdDirect(long channelId, long bannedAccountId) {
        return Optional.ofNullable(channelBanMapper.selectOne(new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId)
                        .eq(ChannelBanEntity::getBannedAccountId, bannedAccountId)))
                .map(ChannelBanEntity::toRecord);
    }

    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        String lookupKey = relationKey(channelId, bannedAccountId);
        Optional<ChannelBanRecord> ban = databaseCache.loadByLookup(
                lookupKey,
                DOMAIN,
                ChannelBanRecord.class,
                ChannelBanRecord::banId,
                () -> findByChannelIdAndBannedAccountIdDirect(channelId, bannedAccountId),
                this::queryById);
        // 指针指向的封禁记录与请求关系不一致时，清理过期指针并回退数据库权威查询。
        if (ban.isPresent() && (ban.get().channelId() != channelId || ban.get().bannedAccountId() != bannedAccountId)) {
            databaseCache.evict(lookupKey);
            return findByChannelIdAndBannedAccountIdDirect(channelId, bannedAccountId);
        }
        return ban;
    }

    @Override
    public PageResult<ChannelBanRecord> pageByChannelId(long channelId, PageQuery query) {
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
    public boolean insert(ChannelBanRecord record) {
        int affectedRows = channelBanMapper.insert(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
        return affectedRows == 1;
    }

    @Override
    @Transactional
    public boolean insertAll(List<ChannelBanRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = channelBanMapper.insertAll(records.stream().map(ChannelBanEntity::from).toList());
        for (ChannelBanRecord record : records) {
            evictRecord(record.banId());
            evictRelation(record.channelId(), record.bannedAccountId());
        }
        return affectedRows == records.size();
    }

    @Override
    @Transactional
    public boolean deleteById(long banId) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled() ? queryById(banId) : Optional.empty();
        int affectedRows = channelBanMapper.deleteById(banId);
        evictRecord(banId);
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
        return affectedRows == 1;
    }

    @Override
    @Transactional
    public boolean deleteByIds(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return true;
        }
        // 删除前读取旧记录，删除成功后逐条精确失效实体与关系指针。
        List<ChannelBanRecord> previous = databaseCache.enabled() ? queryByIds(banIds) : List.of();
        int affectedRows = channelBanMapper.deleteByIds(banIds);
        for (Long banId : banIds) {
            evictRecord(banId);
        }
        for (ChannelBanRecord record : previous) {
            evictRelation(record.channelId(), record.bannedAccountId());
        }
        return affectedRows > 0;
    }

    @Override
    public Optional<ChannelBanRecord> findById(long banId) {
        return queryById(banId);
    }

    @Override
    public List<ChannelBanRecord> findByIds(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return List.of();
        }
        return queryByIds(banIds);
    }

    @Override
    @Transactional
    public boolean update(ChannelBanRecord record) {
        Optional<ChannelBanRecord> previous = databaseCache.enabled()
                ? queryById(record.banId())
                : Optional.empty();
        int affectedRows = channelBanMapper.updateById(ChannelBanEntity.from(record));
        evictRecord(record.banId());
        evictRelation(record.channelId(), record.bannedAccountId());
        previous.ifPresent(old -> evictRelation(old.channelId(), old.bannedAccountId()));
        return affectedRows == 1;
    }

    @Override
    public PageResult<ChannelBanRecord> page(PageQuery query) {
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
                ? queryByChannelId(channelId)
                : List.of();
        channelBanMapper.delete(new LambdaQueryWrapper<ChannelBanEntity>()
                .eq(ChannelBanEntity::getChannelId, channelId));
        for (ChannelBanRecord record : previous) {
            evictRecord(record.banId());
            evictRelation(record.channelId(), record.bannedAccountId());
        }
    }

    private Optional<ChannelBanRecord> queryById(long banId) {
        return Optional.ofNullable(channelBanMapper.selectById(banId))
                .map(ChannelBanEntity::toRecord);
    }

    private List<ChannelBanRecord> queryByIds(List<Long> banIds) {
        return channelBanMapper.selectByIds(banIds).stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    private List<ChannelBanRecord> queryByChannelId(long channelId) {
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
