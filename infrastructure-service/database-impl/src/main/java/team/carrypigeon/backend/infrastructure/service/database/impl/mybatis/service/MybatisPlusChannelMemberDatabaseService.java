package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;

/**
 * MyBatis-Plus 频道成员数据库服务。
 * 职责：在 database-impl 中完成频道成员的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载成员管理业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelMemberDatabaseService implements ChannelMemberDatabaseService {

    private final ChannelMemberMapper channelMemberMapper;

    public MybatisPlusChannelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        this.channelMemberMapper = channelMemberMapper;
    }

    /**
     * 插入新的频道成员关系。
     */
    @Override
    public void insert(ChannelMemberRecord record) {
        channelMemberMapper.insert(ChannelMemberEntity.from(record));
    }

    /**
     * 按频道与账户定位成员关系。
     */
    @Override
    public Optional<ChannelMemberRecord> findByRelation(long channelId, long accountId) {
        return Optional.ofNullable(channelMemberMapper.selectOne(new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getChannelId, channelId)
                        .eq(ChannelMemberEntity::getAccountId, accountId)))
                .map(ChannelMemberEntity::toRecord);
    }

    /**
     * 更新既有频道成员关系。
     */
    @Override
    public void update(ChannelMemberRecord record) {
        channelMemberMapper.updateById(ChannelMemberEntity.from(record));
    }

    /**
     * 按成员关系 ID 删除成员关系。
     */
    @Override
    public void delete(long memberId) {
        channelMemberMapper.deleteById(memberId);
    }

    /**
     * 删除频道下的全部成员关系。
     */
    @Override
    public void deleteByChannelId(long channelId) {
        channelMemberMapper.delete(new LambdaQueryWrapper<ChannelMemberEntity>()
                .eq(ChannelMemberEntity::getChannelId, channelId));
    }

    /**
     * 查询频道下的全部成员关系。
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
     * 查询账户参与的全部频道成员关系。
     */
    @Override
    public List<ChannelMemberRecord> findMembersByAccountId(long accountId) {
        return channelMemberMapper.selectList(new LambdaQueryWrapper<ChannelMemberEntity>()
                        .eq(ChannelMemberEntity::getAccountId, accountId))
                .stream()
                .map(ChannelMemberEntity::toRecord)
                .toList();
    }
}
