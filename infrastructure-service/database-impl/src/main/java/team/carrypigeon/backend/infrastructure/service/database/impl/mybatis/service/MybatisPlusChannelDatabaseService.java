package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;

/**
 * MyBatis-Plus 频道数据库服务。
 * 职责：在 database-impl 中完成频道最小查询能力。
 * 边界：只负责数据库记录映射，不承载频道业务规则。
 */
public class MybatisPlusChannelDatabaseService implements ChannelDatabaseService {

    private final ChannelMapper channelMapper;

    public MybatisPlusChannelDatabaseService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    public List<ChannelRecord> findSystemChannels() {
        return channelMapper.selectList(new LambdaQueryWrapper<ChannelEntity>()
                .eq(ChannelEntity::getSystemChannel, true)
            )
            .stream()
            .map(ChannelEntity::toRecord)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ChannelRecord> findById(long channelId) {
        return Optional.ofNullable(channelMapper.selectById(channelId)).map(ChannelEntity::toRecord);
    }

    @Override
    public List<ChannelRecord> findByIds(Collection<Long> channelIds) {
        return channelMapper.selectList(new LambdaQueryWrapper<ChannelEntity>()
                .in(ChannelEntity::getChannelId, channelIds)
            )
            .stream()
            .map(ChannelEntity::toRecord)
            .collect(Collectors.toList());
    }

    @Override
    public List<ChannelDiscoverRecord> discoverChannels(String keyword, Long cursorChannelId, int limit) {
        // TODO 完善channel查询
        return List.of();
    }

    @Override
    public void insert(ChannelRecord record) {
        channelMapper.insert(ChannelEntity.from(record));
    }

    @Override
    public void update(ChannelRecord record) {
        channelMapper.updateById(ChannelEntity.from(record));
    }

    @Override
    public void delete(long channelId) {
        channelMapper.deleteById(channelId);
    }
}
