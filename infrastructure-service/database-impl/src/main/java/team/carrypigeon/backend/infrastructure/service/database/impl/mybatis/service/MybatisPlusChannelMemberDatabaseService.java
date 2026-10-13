package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelOwnerProjection;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.support.SqlInClauseBatches;

/**
 * MyBatis-Plus 频道成员数据库服务。
 * 职责：在 database-impl 中完成频道成员最小读写能力。
 * 边界：只负责数据库记录映射，不承载成员业务规则。
 */
public class MybatisPlusChannelMemberDatabaseService implements ChannelMemberDatabaseService {

    private final ChannelMemberMapper channelMemberMapper;

    public MybatisPlusChannelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        this.channelMemberMapper = channelMemberMapper;
    }

    @Override
    public void insert(ChannelMemberRecord record) {
        channelMemberMapper.insert(ChannelMemberEntity.from(record));
    }

    @Override
    public Optional<ChannelMemberRecord> findByRelation(long channelId, long accountId) {
        ChannelMemberEntity channelMemberEntity = channelMemberMapper.selectOne(
            new LambdaQueryWrapper<ChannelMemberEntity>()
                .eq(ChannelMemberEntity::getChannelId, channelId)
                .eq(ChannelMemberEntity::getAccountId, accountId)
        );
        if (channelMemberEntity == null) {
            return Optional.empty();
        }
        return Optional.of(channelMemberEntity.toRecord());
    }

    @Override
    public void update(ChannelMemberRecord record) {
        channelMemberMapper.updateById(ChannelMemberEntity.from(record));
    }

    @Override
    public void delete(long memberId) {
        channelMemberMapper.deleteById(memberId);

    }

    @Override
    public void deleteByChannelId(long channelId) {
        channelMemberMapper.delete(
            new LambdaQueryWrapper<ChannelMemberEntity>()
                .eq(ChannelMemberEntity::getChannelId, channelId)
        );
    }

    @Override
    public List<ChannelMemberRecord> findMembersByChannelId(long channelId) {
        return channelMemberMapper.selectList(
                new LambdaQueryWrapper<ChannelMemberEntity>()
                    .eq(ChannelMemberEntity::getChannelId, channelId)
            ).stream()
            .map(ChannelMemberEntity::toRecord)
            .collect(Collectors.toList());
    }

    @Override
    public List<ChannelMemberRecord> findMembersByAccountId(long accountId) {
        return channelMemberMapper.selectList(
                new LambdaQueryWrapper<ChannelMemberEntity>()
                    .eq(ChannelMemberEntity::getAccountId, accountId)
            ).stream()
            .map(ChannelMemberEntity::toRecord)
            .collect(Collectors.toList());
    }
}
