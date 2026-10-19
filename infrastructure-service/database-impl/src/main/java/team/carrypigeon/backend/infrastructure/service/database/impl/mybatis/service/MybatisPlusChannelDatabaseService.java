package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;

/**
 * MyBatis-Plus 频道数据库服务。
 * 职责：在 database-impl 中完成频道的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载频道业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelDatabaseService implements ChannelDatabaseService {

    private final ChannelMapper channelMapper;

    public MybatisPlusChannelDatabaseService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    /**
     * 查询全部 system 频道。
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
        return Optional.ofNullable(channelMapper.selectById(channelId))
                .map(ChannelEntity::toRecord);
    }

    /**
     * 批量读取频道。
     */
    @Override
    public List<ChannelRecord> findByIds(Collection<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return List.of();
        }
        return channelMapper.selectByIds(channelIds).stream()
                .map(ChannelEntity::toRecord)
                .toList();
    }

    /**
     * 按发现页规则查询频道集合。
     */
    @Override
    public List<ChannelDiscoverRecord> discoverChannels(String keyword, Long cursorChannelId, int limit) {
        // TODO 完善频道发现页查询
        return List.of();
    }

    /**
     * 插入新的频道记录。
     */
    @Override
    public void insert(ChannelRecord record) {
        channelMapper.insert(ChannelEntity.from(record));
    }

    /**
     * 更新既有频道记录。
     */
    @Override
    public void update(ChannelRecord record) {
        channelMapper.updateById(ChannelEntity.from(record));
    }

    /**
     * 按频道 ID 删除频道记录。
     */
    @Override
    public void delete(long channelId) {
        channelMapper.deleteById(channelId);
    }
}
