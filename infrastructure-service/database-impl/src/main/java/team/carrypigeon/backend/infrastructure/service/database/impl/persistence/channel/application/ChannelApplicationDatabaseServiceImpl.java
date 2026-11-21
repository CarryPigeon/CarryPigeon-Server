package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.application;

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

/**
 * MyBatis-Plus 频道申请数据库服务。
 * 职责：在 database-impl 中完成频道申请的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载申请审批业务规则。
 */
public class ChannelApplicationDatabaseServiceImpl implements ChannelApplicationDatabaseService {

    private final ChannelApplicationMapper channelApplicationMapper;

    public ChannelApplicationDatabaseServiceImpl(ChannelApplicationMapper channelApplicationMapper) {
        this.channelApplicationMapper = channelApplicationMapper;
    }

    @Override
    public Optional<ChannelApplicationRecord> findByChannelIdAndApplicantId(long channelId, long applicantId) {
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
    public PageResult<ChannelApplicationRecord> pageByChannelId(long channelId, PageQuery query) {
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

    @Override
    public PageResult<ChannelApplicationRecord> pageByApplicantAccountId(long applicantId, PageQuery page) {
        IPage<ChannelApplicationEntity> result = channelApplicationMapper.selectPage(
                new Page<>(page.page(), page.pageSize(), true),
                new LambdaQueryWrapper<ChannelApplicationEntity>()
                        .eq(ChannelApplicationEntity::getApplicantId, applicantId)
                        .orderByDesc(ChannelApplicationEntity::getApplicationId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(ChannelApplicationEntity::toRecord).toList(),
                page.page(),
                page.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 插入新的申请记录。
     */
    @Override
    public boolean insert(ChannelApplicationRecord record) {
        int affectedRows = channelApplicationMapper.insert(ChannelApplicationEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 批量插入申请记录。
     */
    @Override
    public boolean insertAll(List<ChannelApplicationRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = channelApplicationMapper.insertAll(records.stream().map(ChannelApplicationEntity::from).toList());
        return affectedRows == records.size();
    }

    /**
     * 按申请 ID 批量删除记录。
     */
    @Override
    public boolean deleteByIds(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return true;
        }
        int affectedRows = channelApplicationMapper.deleteByIds(applicationIds);
        return affectedRows > 0;
    }

    /**
     * 分页查询申请记录。
     */
    @Override
    public PageResult<ChannelApplicationRecord> page(PageQuery query) {
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
    public boolean deleteById(long applicationId) {
        int affectedRows = channelApplicationMapper.deleteById(applicationId);
        return affectedRows == 1;
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
