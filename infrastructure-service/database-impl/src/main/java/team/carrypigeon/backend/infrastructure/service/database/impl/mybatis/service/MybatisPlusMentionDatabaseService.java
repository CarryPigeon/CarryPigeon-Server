package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMentionDatabaseService implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MybatisPlusMentionDatabaseService(MentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
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
        return Optional.ofNullable(mentionMapper.selectById(mentionId))
                .map(MentionEntity::toRecord);
    }

    /**
     * 查询提及表全部记录。
     */
    @Override
    public List<MentionRecord> list() {
        return mentionMapper.selectList(null).stream()
                .map(MentionEntity::toRecord)
                .toList();
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
