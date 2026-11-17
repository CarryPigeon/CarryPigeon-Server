package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;

/**
 * MyBatis-Plus 提及数据库服务。
 * 职责：在 database-impl 中提供提及表级读写与通用查询。
 * 边界：不承载已读状态流转；read 过滤值由 Repository 传入。
 */
@Component
public class MybatisPlusMentionDatabaseService implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MybatisPlusMentionDatabaseService(MentionMapper mentionMapper) {
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
    public void insert(MentionRecord record) {
        mentionMapper.insert(MentionEntity.from(record));
    }

    /**
     * 批量写入提及记录；空集合不访问 Mapper。
     */
    @Override
    public void insertAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        mentionMapper.insertAll(records.stream().map(MentionEntity::from).toList());
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
    public PageResult<MentionRecord> list(PageQuery query) {
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
    public void update(MentionRecord record) {
        mentionMapper.updateById(MentionEntity.from(record));
    }

    /**
     * 批量更新完整提及记录。
     */
    @Override
    public void updateAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        mentionMapper.saveAll(records.stream().map(MentionEntity::from).toList());
    }

    /**
     * 按提及 ID 删除记录。
     */
    @Override
    public void delete(long mentionId) {
        mentionMapper.deleteById(mentionId);
    }

    /**
     * 按提及 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> mentionIds) {
        if (mentionIds == null || mentionIds.isEmpty()) {
            return;
        }
        mentionMapper.deleteByIds(mentionIds);
    }

    /**
     * 删除指定消息产生的全部提及记录。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        mentionMapper.delete(new LambdaQueryWrapper<MentionEntity>()
                .eq(MentionEntity::getMessageId, messageId));
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
