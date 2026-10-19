package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;

/**
 * MyBatis-Plus 提及数据库服务。
 * 职责：在 database-impl 中提供提及记录的最小写入、读取与已读更新能力。
 * 边界：只负责数据库记录与实体映射，不承载提及解析规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMentionDatabaseService implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MybatisPlusMentionDatabaseService(MentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
    }

    /**
     * 批量写入提及记录；空集合不访问 Mapper。
     */
    @Override
    public void insertAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        mentionMapper.insertAll(records.stream()
                .map(MentionEntity::from)
                .toList());
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
     * 查询账户的提及记录流。
     * 输出：按提及 ID 倒序返回，最多 limit 条。
     */
    @Override
    public List<MentionRecord> findByAccountId(
            long accountId,
            Long cursorMentionId,
            int limit,
            boolean unreadOnly,
            Long channelId
    ) {
        return mentionMapper.selectList(new LambdaQueryWrapper<MentionEntity>()
                        .eq(MentionEntity::getTargetAccountId, accountId)
                        .lt(cursorMentionId != null, MentionEntity::getMentionId, cursorMentionId)
                        .eq(unreadOnly, MentionEntity::getRead, false)
                        .eq(channelId != null, MentionEntity::getChannelId, channelId)
                        .orderByDesc(MentionEntity::getMentionId)
                        .last("LIMIT " + limit))
                .stream()
                .map(MentionEntity::toRecord)
                .toList();
    }

    /**
     * 将单条提及标记为已读。
     * 输出：返回是否实际更新了记录。
     */
    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return mentionMapper.update(null, new LambdaUpdateWrapper<MentionEntity>()
                .eq(MentionEntity::getTargetAccountId, accountId)
                .eq(MentionEntity::getMentionId, mentionId)
                .set(MentionEntity::getRead, true)) > 0;
    }

    /**
     * 批量标记提及为已读。
     * 输出：返回实际更新数量。
     */
    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        return mentionMapper.update(null, new LambdaUpdateWrapper<MentionEntity>()
                .eq(MentionEntity::getTargetAccountId, accountId)
                .eq(MentionEntity::getRead, false)
                .le(beforeMentionId != null, MentionEntity::getMentionId, beforeMentionId)
                .eq(channelId != null, MentionEntity::getChannelId, channelId)
                .set(MentionEntity::getRead, true));
    }
}
