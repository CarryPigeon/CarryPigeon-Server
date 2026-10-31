package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
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
     * 分页查询频道下的封禁记录。
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
     * 分页查询封禁记录。
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

    /**
     * 删除频道下的全部封禁记录。
     */
    @Override
    public void deleteByChannelId(long channelId) {
        channelBanMapper.delete(new LambdaQueryWrapper<ChannelBanEntity>()
                .eq(ChannelBanEntity::getChannelId, channelId));
    }
}
