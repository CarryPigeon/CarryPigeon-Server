package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelUnreadProjection;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;

/**
 * MyBatis-Plus 频道已读状态数据库服务。
 * 职责：在 database-impl 中持久化频道已读锚点与未读统计查询。
 * 边界：不承载未读业务规则，只暴露存储层读写能力。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelReadStateDatabaseService implements ChannelReadStateDatabaseService {

    private final ChannelReadStateMapper channelReadStateMapper;

    public MybatisPlusChannelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    /**
     * 查询账户在频道中的已读状态记录。
     */
    @Override
    public Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
        return Optional.ofNullable(channelReadStateMapper.selectOne(new LambdaQueryWrapper<ChannelReadStateEntity>()
                        .eq(ChannelReadStateEntity::getChannelId, channelId)
                        .eq(ChannelReadStateEntity::getAccountId, accountId)))
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
     * 仅当新锚点更靠后时推进已读状态。
     * 输出：实际推进时返回 true。
     */
    @Override
    public boolean advanceIfNewer(ChannelReadStateRecord record) {
        return channelReadStateMapper.advanceIfNewer(ChannelReadStateEntity.from(record)) > 0;
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
