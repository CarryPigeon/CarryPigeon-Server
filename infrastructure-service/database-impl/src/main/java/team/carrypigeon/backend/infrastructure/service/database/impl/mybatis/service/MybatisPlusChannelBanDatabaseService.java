package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;

/**
 * MyBatis-Plus 频道封禁数据库服务。
 * 职责：在 database-impl 中完成频道封禁的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载封禁业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelBanDatabaseService implements ChannelBanDatabaseService {

    private final ChannelBanMapper channelBanMapper;

    public MybatisPlusChannelBanDatabaseService(ChannelBanMapper channelBanMapper) {
        this.channelBanMapper = channelBanMapper;
    }

    /**
     * 查询频道对目标账户的封禁记录。
     */
    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        return Optional.ofNullable(channelBanMapper.selectOne(new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId)
                        .eq(ChannelBanEntity::getBannedAccountId, bannedAccountId)))
                .map(ChannelBanEntity::toRecord);
    }

    /**
     * 查询频道下全部封禁记录。
     * 输出：按创建时间倒序排列。
     */
    @Override
    public List<ChannelBanRecord> findByChannelId(long channelId) {
        return channelBanMapper.selectList(new LambdaQueryWrapper<ChannelBanEntity>()
                        .eq(ChannelBanEntity::getChannelId, channelId)
                        .orderByDesc(ChannelBanEntity::getCreatedAt))
                .stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    /**
     * 插入新的封禁记录。
     */
    @Override
    public void insert(ChannelBanRecord record) {
        channelBanMapper.insert(ChannelBanEntity.from(record));
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
    }

    /**
     * 按封禁 ID 删除记录。
     */
    @Override
    public void delete(long banId) {
        channelBanMapper.deleteById(banId);
    }

    /**
     * 按封禁 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return;
        }
        channelBanMapper.deleteByIds(banIds);
    }

    /**
     * 查询封禁表全部记录。
     */
    @Override
    public List<ChannelBanRecord> list() {
        return channelBanMapper.selectList(null).stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    /**
     * 按封禁 ID 查询记录。
     */
    @Override
    public Optional<ChannelBanRecord> findById(long banId) {
        return Optional.ofNullable(channelBanMapper.selectById(banId))
                .map(ChannelBanEntity::toRecord);
    }

    /**
     * 按封禁 ID 批量查询记录。
     */
    @Override
    public List<ChannelBanRecord> findByIds(List<Long> banIds) {
        if (banIds == null || banIds.isEmpty()) {
            return List.of();
        }
        return channelBanMapper.selectByIds(banIds).stream()
                .map(ChannelBanEntity::toRecord)
                .toList();
    }

    /**
     * 更新既有封禁记录。
     * 约束：按频道与被封禁账户定位，不依赖封禁 ID。
     */
    @Override
    public void update(ChannelBanRecord record) {
        channelBanMapper.updateById(ChannelBanEntity.from(record));
    }
}
