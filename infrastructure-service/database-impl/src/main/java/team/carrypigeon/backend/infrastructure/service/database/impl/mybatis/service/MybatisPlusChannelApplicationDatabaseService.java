package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelApplicationEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelApplicationMapper;

/**
 * MyBatis-Plus 频道申请数据库服务。
 * 职责：在 database-impl 中完成频道申请的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载申请审批业务规则。
 */
public class MybatisPlusChannelApplicationDatabaseService implements ChannelApplicationDatabaseService {

    private final ChannelApplicationMapper channelApplicationMapper;

    public MybatisPlusChannelApplicationDatabaseService(ChannelApplicationMapper channelApplicationMapper) {
        this.channelApplicationMapper = channelApplicationMapper;
    }

    /** 在外层清理事务中限量删除申请记录，失败时随本批一起回滚。 */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ChannelApplicationRecord> deleteBatchByChannelId(long channelId, int limit) {
        List<ChannelApplicationRecord> records = channelApplicationMapper.selectList(new LambdaQueryWrapper<ChannelApplicationEntity>()
                .eq(ChannelApplicationEntity::getChannelId, channelId)
                .orderByAsc(ChannelApplicationEntity::getApplicationId)
                .last("LIMIT " + limit))
                .stream().map(ChannelApplicationEntity::toRecord).toList();
        deleteAll(records.stream().map(ChannelApplicationRecord::applicationId).toList());
        return records;
    }

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
     * 分页查询频道下的申请记录。
     */
    @Override
    public PageResult<ChannelApplicationRecord> pageApplicationsByChannelId(long channelId, PageQuery query) {
        IPage<ChannelApplicationEntity> result = channelApplicationMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelApplicationEntity>()
                        .eq(ChannelApplicationEntity::getChannelId, channelId)
                        .orderByDesc(ChannelApplicationEntity::getApplicationId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelApplicationEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 插入新的申请记录。
     */
    @Override
    public void insert(ChannelApplicationRecord record) {
        channelApplicationMapper.insert(ChannelApplicationEntity.from(record));
    }

    /**
     * 批量插入申请记录。
     */
    @Override
    public void insertAll(List<ChannelApplicationRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        channelApplicationMapper.insertAll(records.stream().map(ChannelApplicationEntity::from).toList());
    }

    /**
     * 按申请 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return;
        }
        channelApplicationMapper.deleteByIds(applicationIds);
    }

    /**
     * 分页查询申请记录。
     */
    @Override
    public PageResult<ChannelApplicationRecord> list(PageQuery query) {
        IPage<ChannelApplicationEntity> result = channelApplicationMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<ChannelApplicationEntity>()
                        .orderByAsc(ChannelApplicationEntity::getApplicationId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelApplicationEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按申请 ID 批量查询记录。
     */
    @Override
    public List<ChannelApplicationRecord> findByIds(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return List.of();
        }
        return channelApplicationMapper.selectByIds(applicationIds).stream()
                .map(ChannelApplicationEntity::toRecord)
                .toList();
    }

    /**
     * 由数据库条件更新取得唯一审批权，避免事务旧快照或并发请求覆盖已经提交的决定。
     */
    @Override
    public boolean decideIfPending(ChannelApplicationRecord record) {
        return channelApplicationMapper.update(new LambdaUpdateWrapper<ChannelApplicationEntity>()
                .eq(ChannelApplicationEntity::getApplicationId, record.applicationId())
                .eq(ChannelApplicationEntity::getChannelId, record.channelId())
                .eq(ChannelApplicationEntity::getStatus, "PENDING")
                .set(ChannelApplicationEntity::getStatus, record.status())
                .set(ChannelApplicationEntity::getRespondedAt, record.respondedAt())) == 1;
    }

    /**
     * 按申请 ID 删除申请记录。
     */
    @Override
    public void delete(long applicationId) {
        channelApplicationMapper.deleteById(applicationId);
    }

    /**
     * 删除频道下的全部申请记录。
     */
    @Override
    public void deleteByChannelId(long channelId) {
        channelApplicationMapper.delete(new LambdaQueryWrapper<ChannelApplicationEntity>()
                .eq(ChannelApplicationEntity::getChannelId, channelId));
    }
}
