package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.member;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;

/**
 * MyBatis-Plus 频道成员数据库服务。
 * 职责：在 database-impl 中完成频道成员的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载成员管理业务规则。
 * 缓存：按 (频道, 账户) 唯一键缓存“关系 -&gt; 成员主键”lookup 指针，成员实体按主键缓存一份；
 * 写路径精确失效实体键与新旧关系指针，成员列表/分页查询不缓存。
 */
public class ChannelMemberDatabaseServiceImpl implements ChannelMemberDatabaseService {

    private static final String DOMAIN = "channel-member";
    private static final String LOOKUP_RELATION = "relation";

    private final ChannelMemberMapper channelMemberMapper;
    private final DatabaseCache databaseCache;

    /**
     * 创建频道成员数据库服务。
     *
     * @param channelMemberMapper 频道成员 Mapper
     * @param databaseCache 数据库查询缓存门面
     */
    public ChannelMemberDatabaseServiceImpl(
            ChannelMemberMapper channelMemberMapper,
            DatabaseCache databaseCache
    ) {
        this.channelMemberMapper = channelMemberMapper;
        this.databaseCache = databaseCache;
    }

    @Override
    public List<Long> filterMemberAccountIds(long channelId, List<Long> accountIds) {
        // 在线账号较多时限制单条 IN 大小，查询量随在线候选数变化，不加载频道全部离线成员。
        List<Long> members = new ArrayList<>();
        for (int start = 0; start < accountIds.size(); start += 1000) {
            members.addAll(channelMemberMapper.filterMemberAccountIds(channelId,
                    accountIds.subList(start, Math.min(start + 1000, accountIds.size()))));
        }
        return members;
    }

    @Override
    public List<Long> findChannelIdsByAccountId(long accountId) {
        return channelMemberMapper.findChannelIdsByAccountId(accountId);
    }

    /** 授权数据只从数据库读取，避免缓存失效延迟继续授予旧权限。 */
    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountIdDirect(long channelId, long accountId) {
        return Optional.ofNullable(channelMemberMapper.selectOne(new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getChannelId, channelId)
                        .eq(ChannelMemberEntity::getAccountId, accountId)))
                .map(ChannelMemberEntity::toRecord);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountIdForUpdate(long channelId, long accountId) {
        // 即使外层事务已建立可重复读快照，角色校验仍读取前一笔治理事务提交后的最新关系。
        return Optional.ofNullable(channelMemberMapper.selectOne(new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getChannelId, channelId)
                        .eq(ChannelMemberEntity::getAccountId, accountId)
                        .last("FOR UPDATE")))
                .map(ChannelMemberEntity::toRecord);
    }

    /**
     * 插入新的频道成员关系。
     */
    @Override
    @Transactional
    public boolean insert(ChannelMemberRecord record) {
        int affectedRows = channelMemberMapper.insert(ChannelMemberEntity.from(record));
        evictRecord(record.memberId());
        evictRelation(record.channelId(), record.accountId());
        return affectedRows == 1;
    }

    /**
     * 批量插入频道成员关系。
     */
    @Override
    @Transactional
    public boolean insertAll(List<ChannelMemberRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = channelMemberMapper.insertAll(records.stream().map(ChannelMemberEntity::from).toList());
        for (ChannelMemberRecord record : records) {
            evictRecord(record.memberId());
            evictRelation(record.channelId(), record.accountId());
        }
        return affectedRows == records.size();
    }
    /**
     * 按成员关系 ID 查询。
     */
    @Override
    public Optional<ChannelMemberRecord> findById(long memberId) {
        return queryById(memberId);
    }

    /**
     * 按成员关系 ID 批量查询。
     */
    @Override
    public List<ChannelMemberRecord> findByIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return queryByIds(memberIds);
    }

    /**
     * 按频道与账户定位成员关系。
     * 缓存：先查 relation lookup 指针，命中后按成员主键读实体缓存；未命中时一次数据库查询同时回填指针与实体。
     * 仅用于可接受最终一致性的展示查询；权限判断使用 findByChannelIdAndAccountIdDirect。
     */
    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
        String lookupKey = relationKey(channelId, accountId);
        Optional<ChannelMemberRecord> member = databaseCache.loadByLookup(
                lookupKey,
                DOMAIN,
                ChannelMemberRecord.class,
                ChannelMemberRecord::memberId,
                () -> findByChannelIdAndAccountIdDirect(channelId, accountId),
                this::queryById);
        // 指针指向的成员记录与请求关系不一致时，清理过期指针并回退数据库权威查询。
        if (member.isPresent()
                && (member.get().channelId() != channelId || member.get().accountId() != accountId)) {
            databaseCache.evict(lookupKey);
            return findByChannelIdAndAccountIdDirect(channelId, accountId);
        }
        return member;
    }

    /**
     * 更新既有频道成员关系。
     * 缓存：更新前读取旧记录，写入成功后精确失效新实体、旧/新关系指针。
     */
    @Override
    @Transactional
    public boolean update(ChannelMemberRecord record) {
        Optional<ChannelMemberRecord> previous = databaseCache.enabled()
                ? queryById(record.memberId())
                : Optional.empty();
        int affectedRows = channelMemberMapper.updateById(ChannelMemberEntity.from(record));
        evictRecord(record.memberId());
        evictRelation(record.channelId(), record.accountId());
        previous.ifPresent(old -> evictRelation(old.channelId(), old.accountId()));
        return affectedRows == 1;
    }

    /**
     * 分页查询频道成员记录。
     * 缓存：分页查询键空间无界，不参与缓存，保留给管理后台/离线维护使用。
     */
    @Override
    public PageResult<ChannelMemberRecord> page(PageQuery query) {
        IPage<ChannelMemberEntity> result = channelMemberMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelMemberEntity>()
                        .orderByAsc(ChannelMemberEntity::getMemberId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelMemberEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按成员关系 ID 删除成员关系。
     * 缓存：删除前读取旧记录，删除成功后精确失效实体与关系指针。
     */
    @Override
    @Transactional
    public boolean deleteById(long memberId) {
        Optional<ChannelMemberRecord> previous = databaseCache.enabled()
                ? queryById(memberId)
                : Optional.empty();
        int affectedRows = channelMemberMapper.deleteById(memberId);
        evictRecord(memberId);
        previous.ifPresent(old -> evictRelation(old.channelId(), old.accountId()));
        return affectedRows == 1;
    }

    /**
     * 按成员关系 ID 批量删除。
     */
    @Override
    @Transactional
    public boolean deleteByIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return true;
        }
        // 删除前读取旧记录，删除成功后逐条精确失效实体与关系指针。
        List<ChannelMemberRecord> previous = databaseCache.enabled() ? queryByIds(memberIds) : List.of();
        int affectedRows = channelMemberMapper.deleteByIds(memberIds);
        for (Long memberId : memberIds) {
            evictRecord(memberId);
        }
        for (ChannelMemberRecord record : previous) {
            evictRelation(record.channelId(), record.accountId());
        }
        return affectedRows > 0;
    }

    /**
     * 删除频道下的全部成员关系。
     * 缓存：缓存启用时先读取该频道成员快照，删除成功后逐条精确失效实体与关系指针。
     */
    @Override
    @Transactional
    public void deleteByChannelId(long channelId) {
        List<ChannelMemberRecord> previous = databaseCache.enabled()
                ? findMembersByChannelId(channelId)
                : List.of();
        channelMemberMapper.delete(new LambdaQueryWrapper<ChannelMemberEntity>()
                .eq(ChannelMemberEntity::getChannelId, channelId));
        for (ChannelMemberRecord record : previous) {
            evictRecord(record.memberId());
            evictRelation(record.channelId(), record.accountId());
        }
    }

    /**
     * 查询频道下的全部成员关系。
     * 说明：显式全量快照，供通知接收人与治理快照使用；结果集大且复用低，不属于当前缓存覆盖范围。
     */
    @Override
    public List<ChannelMemberRecord> findMembersByChannelId(long channelId) {
        return channelMemberMapper.selectList(new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getChannelId, channelId))
                .stream()
                .map(ChannelMemberEntity::toRecord)
                .toList();
    }

    /**
     * 分页查询账户参与的全部频道成员关系。
     * 边界：分页键空间无界且复用低，不属于当前缓存覆盖范围。
     */
    @Override
    public PageResult<ChannelMemberRecord> pageByAccountId(long accountId, PageQuery query) {
        IPage<ChannelMemberEntity> result = channelMemberMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getAccountId, accountId)
                        // 频道主记录已经撤下时，待清理成员关系不再参与分页及总数。
                        .exists("SELECT 1 FROM chat_channel c WHERE c.channel_id = chat_channel_member.channel_id")
                        .orderByDesc(ChannelMemberEntity::getChannelId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelMemberEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 分页查询频道下的成员关系。
     * 边界：分页键空间无界且复用低，不属于当前缓存覆盖范围。
     */
    @Override
    public PageResult<ChannelMemberRecord> pageByChannelId(long channelId, PageQuery query) {
        IPage<ChannelMemberEntity> result = channelMemberMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getChannelId, channelId)
                        .orderByDesc(ChannelMemberEntity::getMemberId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelMemberEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    private Optional<ChannelMemberRecord> queryById(long memberId) {
        return Optional.ofNullable(channelMemberMapper.selectById(memberId))
                .map(ChannelMemberEntity::toRecord);
    }

    private List<ChannelMemberRecord> queryByIds(List<Long> memberIds) {
        return channelMemberMapper.selectByIds(memberIds).stream()
                .map(ChannelMemberEntity::toRecord)
                .toList();
    }

    private String relationKey(long channelId, long accountId) {
        return DatabaseCache.lookupKey(DOMAIN, LOOKUP_RELATION, Long.toString(channelId), Long.toString(accountId));
    }

    private void evictRecord(long memberId) {
        databaseCache.evict(DatabaseCache.entityKey(DOMAIN, memberId));
    }

    private void evictRelation(long channelId, long accountId) {
        databaseCache.evict(relationKey(channelId, accountId));
    }
}
