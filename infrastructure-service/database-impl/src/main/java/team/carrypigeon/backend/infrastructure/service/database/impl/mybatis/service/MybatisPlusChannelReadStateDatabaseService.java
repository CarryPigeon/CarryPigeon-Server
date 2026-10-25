package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelUnreadProjection;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;

/**
 * MyBatis-Plus 频道已读状态数据库服务。
 * 职责：在 database-impl 中持久化频道已读锚点与未读统计查询。
 * 边界：不承载“只前进不后退”等业务规则，只暴露存储层读写能力。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelReadStateDatabaseService implements ChannelReadStateDatabaseService {

    private final ChannelReadStateMapper channelReadStateMapper;

    public MybatisPlusChannelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    /**
     * 写入新的已读状态记录。
     */
    @Override
    public void insert(ChannelReadStateRecord record) {
        channelReadStateMapper.insert(ChannelReadStateEntity.from(record));
    }

    /**
     * 批量写入已读状态记录。
     */
    @Override
    public void insertAll(List<ChannelReadStateRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        channelReadStateMapper.insertAll(records.stream().map(ChannelReadStateEntity::from).toList());
    }

    /**
     * 按复合主键删除记录。
     */
    @Override
    public void delete(ChannelReadStateKey key) {
        channelReadStateMapper.delete(new LambdaQueryWrapper<ChannelReadStateEntity>()
                .eq(ChannelReadStateEntity::getChannelId, key.channelId())
                .eq(ChannelReadStateEntity::getAccountId, key.accountId()));
    }

    /**
     * 按复合主键批量删除记录。
     */
    @Override
    public void deleteAll(List<ChannelReadStateKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        channelReadStateMapper.deleteByKeys(keys);
    }

    /**
     * 按频道与账户更新完整已读状态记录。
     */
    @Override
    public void update(ChannelReadStateRecord record) {
        channelReadStateMapper.update(null, new LambdaUpdateWrapper<ChannelReadStateEntity>()
                .eq(ChannelReadStateEntity::getChannelId, record.channelId())
                .eq(ChannelReadStateEntity::getAccountId, record.accountId())
                .set(ChannelReadStateEntity::getLastReadMessageId, record.lastReadMessageId())
                .set(ChannelReadStateEntity::getLastReadTime, record.lastReadTime())
                .set(ChannelReadStateEntity::getCreatedAt, record.createdAt())
                .set(ChannelReadStateEntity::getUpdatedAt, record.updatedAt()));
    }

    /**
     * 查询已读状态表全部记录。
     */
    @Override
    public List<ChannelReadStateRecord> list() {
        return channelReadStateMapper.selectList(null).stream()
                .map(ChannelReadStateEntity::toRecord)
                .toList();
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
        return Optional.ofNullable(channelReadStateMapper.selectByRelationForUpdate(key.channelId(), key.accountId()))
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
        return channelReadStateMapper.findUnreadsByAccountId(accountId).stream()
                .map(ChannelUnreadProjection::toRecord)
                .toList();
    }
}
