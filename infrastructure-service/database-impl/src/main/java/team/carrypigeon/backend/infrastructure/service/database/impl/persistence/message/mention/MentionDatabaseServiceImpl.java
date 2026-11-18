package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.mention;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

/**
 * MyBatis-Plus 提及数据库服务。
 * 职责：在 database-impl 中提供提及表级读写与通用查询。
 * 边界：不承载已读状态流转；read 过滤值由 Repository 传入。
 */
public class MentionDatabaseServiceImpl implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MentionDatabaseServiceImpl(MentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
    }

    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        // 账号与未读谓词始终存在；撤回删除先完成时不会重建记录，更新先完成时仍允许之后删除。
        return mentionMapper.update(null, new LambdaUpdateWrapper<MentionEntity>()
                .eq(MentionEntity::getTargetAccountId, accountId)
                .eq(MentionEntity::getRead, false)
                .lt(beforeMentionId != null, MentionEntity::getMentionId, beforeMentionId)
                .eq(channelId != null, MentionEntity::getChannelId, channelId)
                .set(MentionEntity::getRead, true));
    }

    /**
     * 写入单条提及记录。
     */
    @Override
    public boolean insert(MentionRecord record) {
        int affectedRows = mentionMapper.insert(MentionEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 批量写入提及记录；空集合不访问 Mapper。
     */
    @Override
    public boolean insertAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = mentionMapper.insertAll(records.stream().map(MentionEntity::from).toList());
        return affectedRows == records.size();
    }

    /**
     * 按提及 ID 查询记录。
     */
    @Override
    public Optional<MentionRecord> findById(long mentionId) {
        return Optional.ofNullable(mentionMapper.selectOne(new LambdaQueryWrapper<MentionEntity>()
                        .eq(MentionEntity::getMentionId, mentionId)
                        .exists("SELECT 1 FROM chat_channel c WHERE c.channel_id = chat_mention.channel_id")))
                .map(MentionEntity::toRecord);
    }

    /**
     * 分页查询提及记录。
     */
    @Override
    public PageResult<MentionRecord> page(PageQuery query) {
        IPage<MentionEntity> result = mentionMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<MentionEntity>()
                        .orderByAsc(MentionEntity::getMentionId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(MentionEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按提及 ID 批量查询记录。
     */
    @Override
    public List<MentionRecord> findByIds(List<Long> mentionIds) {
        if (mentionIds == null || mentionIds.isEmpty()) {
            return List.of();
        }
        return mentionMapper.selectByIds(mentionIds).stream()
                .map(MentionEntity::toRecord)
                .toList();
    }

    /**
     * 更新完整提及记录。
     */
    @Override
    public boolean update(MentionRecord record) {
        int affectedRows = mentionMapper.updateById(MentionEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 批量写入完整提及记录；存在时覆盖，不存在时插入。
     */
    @Override
    public boolean updateAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        mentionMapper.saveAll(records.stream().map(MentionEntity::from).toList());
        // MySQL 对插入、修改及未变化记录的受影响行数不同，不能按条数判断是否成功。
        return true;
    }

    /**
     * 按提及 ID 删除记录。
     */
    @Override
    public boolean deleteById(long mentionId) {
        int affectedRows = mentionMapper.deleteById(mentionId);
        return affectedRows == 1;
    }

    /**
     * 按提及 ID 批量删除记录。
     */
    @Override
    public boolean deleteByIds(List<Long> mentionIds) {
        if (mentionIds == null || mentionIds.isEmpty()) {
            return true;
        }
        int affectedRows = mentionMapper.deleteByIds(mentionIds);
        return affectedRows > 0;
    }

    /**
     * 删除指定消息产生的全部提及记录。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        mentionMapper.delete(new LambdaQueryWrapper<MentionEntity>()
                .eq(MentionEntity::getMessageId, messageId));
    }

    @Override
    public void deleteByChannelId(long channelId) {
        mentionMapper.delete(new LambdaQueryWrapper<MentionEntity>()
                .eq(MentionEntity::getChannelId, channelId));
    }

    /**
     * 按通用条件查询提及记录。
     */
    @Override
    public List<MentionRecord> find(MentionQuery query) {
        return mentionMapper.selectList(new LambdaQueryWrapper<MentionEntity>()
                        .eq(MentionEntity::getTargetAccountId, query.accountId())
                        // 删除提交后立即隐藏提及，不等待后台物理清理。
                        .exists("SELECT 1 FROM chat_channel c WHERE c.channel_id = chat_mention.channel_id")
                        .lt(query.beforeMentionId() != null, MentionEntity::getMentionId, query.beforeMentionId())
                        .eq(query.read() != null, MentionEntity::getRead, query.read())
                        .eq(query.channelId() != null, MentionEntity::getChannelId, query.channelId())
                        .orderByDesc(MentionEntity::getMentionId)
                        .last("LIMIT " + query.limit()))
                .stream()
                .map(MentionEntity::toRecord)
                .toList();
    }
}
