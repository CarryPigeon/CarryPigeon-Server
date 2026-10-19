package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelApplicationEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelApplicationMapper;

/**
 * MyBatis-Plus 频道申请数据库服务。
 * 职责：在 database-impl 中完成频道申请的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载申请审批业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusChannelApplicationDatabaseService implements ChannelApplicationDatabaseService {

    private final ChannelApplicationMapper channelApplicationMapper;

    public MybatisPlusChannelApplicationDatabaseService(ChannelApplicationMapper channelApplicationMapper) {
        this.channelApplicationMapper = channelApplicationMapper;
    }

    /**
     * 按频道与申请人定位申请记录。
     */
    @Override
    public Optional<ChannelApplicationRecord> findByChannelAndApplicant(long channelId, long applicantId) {
        return Optional.ofNullable(channelApplicationMapper.selectOne(
                        new LambdaQueryWrapper<ChannelApplicationEntity>()
                                .eq(ChannelApplicationEntity::getChannelId, channelId)
                                .eq(ChannelApplicationEntity::getApplicantId, applicantId)))
                .map(ChannelApplicationEntity::toRecord);
    }

    /**
     * 按申请 ID 查询申请记录。
     */
    @Override
    public Optional<ChannelApplicationRecord> findById(long applicationId) {
        return Optional.ofNullable(channelApplicationMapper.selectById(applicationId))
                .map(ChannelApplicationEntity::toRecord);
    }

    /**
     * 查询频道下的全部申请记录。
     */
    @Override
    public List<ChannelApplicationRecord> findByChannelId(long channelId) {
        return channelApplicationMapper.selectList(new LambdaQueryWrapper<ChannelApplicationEntity>()
                        .eq(ChannelApplicationEntity::getChannelId, channelId))
                .stream()
                .map(ChannelApplicationEntity::toRecord)
                .toList();
    }

    /**
     * 插入新的申请记录。
     */
    @Override
    public void insert(ChannelApplicationRecord record) {
        channelApplicationMapper.insert(ChannelApplicationEntity.from(record));
    }

    /**
     * 更新既有申请记录。
     */
    @Override
    public void update(ChannelApplicationRecord record) {
        channelApplicationMapper.updateById(ChannelApplicationEntity.from(record));
    }

    /**
     * 按申请 ID 删除申请记录。
     */
    @Override
    public void delete(long applicationId) {
        channelApplicationMapper.deleteById(applicationId);
    }
}
