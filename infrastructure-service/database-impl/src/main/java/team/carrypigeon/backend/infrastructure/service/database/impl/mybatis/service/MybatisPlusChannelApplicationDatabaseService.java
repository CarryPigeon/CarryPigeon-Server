package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelApplicationEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelApplicationMapper;

/**
 * MyBatis-Plus 频道邀请数据库服务。
 * 职责：在 database-impl 中完成频道邀请的最小读写能力。
 * 边界：只负责数据库记录映射，不承载邀请业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelApplicationDatabaseService implements ChannelApplicationDatabaseService {

    private final ChannelApplicationMapper channelInviteMapper;

    public MybatisPlusChannelApplicationDatabaseService(ChannelApplicationMapper channelInviteMapper) {
        this.channelInviteMapper = channelInviteMapper;
    }

    @Override
    public Optional<ChannelApplicationRecord> findByChannelAndApplicant(long channelId, long applicantId) {
        ChannelApplicationEntity entity = channelInviteMapper.selectOne(
            new LambdaQueryWrapper<ChannelApplicationEntity>()
                .eq(ChannelApplicationEntity::getChannelId, channelId)
                .eq(ChannelApplicationEntity::getApplicantId, applicantId)
        );
        if(entity==null){
            return Optional.empty();
        }
        return Optional.of(entity.toRecord());
    }

    @Override
    public Optional<ChannelApplicationRecord> findById(long applicationId) {
        ChannelApplicationEntity entity = channelInviteMapper.selectById(applicationId);
        if(entity==null){
            return Optional.empty();
        }
        return Optional.of(entity.toRecord());
    }

    @Override
    public List<ChannelApplicationRecord> findByChannelId(long channelId) {
        return channelInviteMapper.selectList(
                new LambdaQueryWrapper<ChannelApplicationEntity>()
                    .eq(ChannelApplicationEntity::getChannelId, channelId)
            )
            .stream()
            .map(ChannelApplicationEntity::toRecord)
            .collect(Collectors.toList());
    }

    @Override
    public void insert(ChannelApplicationRecord record) {
        channelInviteMapper.insert(ChannelApplicationEntity.from(record));
    }

    @Override
    public void update(ChannelApplicationRecord record) {
        channelInviteMapper.update(ChannelApplicationEntity.from(record), new LambdaUpdateWrapper<ChannelApplicationEntity>().eq(ChannelApplicationEntity::getApplicationId, record.applicationId()));
    }
}
