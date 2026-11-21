package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.readstate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;

/**
 * MyBatis-Plus 频道已读状态数据库服务。
 * 职责：在 database-impl 中持久化频道已读锚点与未读统计查询。
 * 边界：不承载“只前进不后退”等业务规则，只暴露存储层读写能力。
 */
public class ChannelReadStateDatabaseServiceImpl implements ChannelReadStateDatabaseService {

    private final ChannelReadStateMapper channelReadStateMapper;

    public ChannelReadStateDatabaseServiceImpl(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    /**
     * 写入新的已读状态记录。
     */
    @Override
    public boolean insert(ChannelReadStateRecord record) {
        int affectedRows = channelReadStateMapper.insert(ChannelReadStateEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 批量写入已读状态记录。
     */
    @Override
    public boolean insertAll(List<ChannelReadStateRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = channelReadStateMapper.insertAll(records.stream().map(ChannelReadStateEntity::from).toList());
        return affectedRows == records.size();
    }

    /**
     * 按复合主键删除记录。
     */
    @Override
    public boolean deleteById(ChannelReadStateKey key) {
        return channelReadStateMapper.delete(new LambdaQueryWrapper<ChannelReadStateEntity>()
                .eq(ChannelReadStateEntity::getChannelId, key.channelId())
                .eq(ChannelReadStateEntity::getAccountId, key.accountId())) == 1;
    }

    /**
     * 按复合主键批量删除记录。
     */
    @Override
    public boolean deleteByIds(List<ChannelReadStateKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return true;
        }
        return channelReadStateMapper.deleteByKeys(keys) > 0;
    }

    @Override
    public void deleteByChannelId(long channelId) {
        channelReadStateMapper.delete(new LambdaQueryWrapper<ChannelReadStateEntity>()
                .eq(ChannelReadStateEntity::getChannelId, channelId));
    }

    /**
     * 按频道与账户更新完整已读状态记录。
     */
    @Override
    public boolean update(ChannelReadStateRecord record) {
        int affectedRows = channelReadStateMapper.update(null, new LambdaUpdateWrapper<ChannelReadStateEntity>()
                .eq(ChannelReadStateEntity::getChannelId, record.channelId())
                .eq(ChannelReadStateEntity::getAccountId, record.accountId())
                .set(ChannelReadStateEntity::getLastReadMessageId, record.lastReadMessageId())
                .set(ChannelReadStateEntity::getCreatedAt, record.createdAt())
                .set(ChannelReadStateEntity::getUpdatedAt, record.updatedAt()));
        return affectedRows == 1;
    }

    /**
     * 分页查询已读状态记录。
     */
    @Override
    public PageResult<ChannelReadStateRecord> page(PageQuery query) {
        IPage<ChannelReadStateEntity> result = channelReadStateMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelReadStateEntity>()
                        .orderByAsc(ChannelReadStateEntity::getChannelId)
                        .orderByAsc(ChannelReadStateEntity::getAccountId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelReadStateEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按复合主键查询记录。
     */
    @Override
    public Optional<ChannelReadStateRecord> findById(ChannelReadStateKey key) {
        return Optional.ofNullable(channelReadStateMapper.selectOne(new LambdaQueryWrapper<ChannelReadStateEntity>()
                        .eq(ChannelReadStateEntity::getChannelId, key.channelId())
                        .eq(ChannelReadStateEntity::getAccountId, key.accountId())))
                .map(ChannelReadStateEntity::toRecord);
    }

    /**
     * 按复合主键批量查询记录。
     */
    @Override
    public List<ChannelReadStateRecord> findByIds(List<ChannelReadStateKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return channelReadStateMapper.selectByKeys(keys).stream()
                .map(ChannelReadStateEntity::toRecord)
                .toList();
    }

    /**
     * 以行锁读取账户在频道中的已读状态记录。
     * 约束：必须在事务内调用。
     */
    @Override
    public Optional<ChannelReadStateRecord> findByIdForUpdate(ChannelReadStateKey key) {
        return Optional.ofNullable(channelReadStateMapper.selectOne(new LambdaQueryWrapper<ChannelReadStateEntity>()
                        .eq(ChannelReadStateEntity::getChannelId, key.channelId())
                        .eq(ChannelReadStateEntity::getAccountId, key.accountId())
                        .last("FOR UPDATE")))
                .map(ChannelReadStateEntity::toRecord);
    }

    /**
     * 新增或覆盖频道已读状态记录。
     */
    @Override
    public void upsert(ChannelReadStateRecord record) {
        channelReadStateMapper.upsertState(ChannelReadStateEntity.from(record));
    }

    /**
     * 查询账户各频道未读统计。
     */
    @Override
    public List<ChannelUnreadRecord> findUnreadsByAccountId(long accountId) {
        List<ChannelReadStateMapper.UnreadBoundary> boundaries = channelReadStateMapper.findUnreadBoundaries(accountId);
        List<ChannelUnreadRecord> result = new ArrayList<>();
        // 分批合并为 UNION ALL，避免逐频道网络往返，也限制单条 SQL 的长度。
        for (int start = 0; start < boundaries.size(); start += 100) {
            List<ChannelReadStateMapper.UnreadBoundary> batch = boundaries.subList(start, Math.min(start + 100, boundaries.size()));
            channelReadStateMapper.countUnreads(accountId, batch).stream()
                    .filter(row -> row.getUnreadCount() > 0)
                    .map(ChannelUnreadProjection::toRecord)
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    @Override
    public long countUnreads(long channelId, long accountId, long afterMessageId) {
        var boundary = new ChannelReadStateMapper.UnreadBoundary(channelId, afterMessageId);
        return channelReadStateMapper.countUnreads(accountId, List.of(boundary)).stream()
                .mapToLong(ChannelUnreadProjection::getUnreadCount)
                .findFirst().orElse(0L);
    }
}
