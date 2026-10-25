package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;

/**
 * MyBatis-Plus 消息数据库服务。
 * 职责：在 database-impl 中完成消息持久化投影的最小读写能力。
 * 边界：只负责持久化投影与实体之间的映射，不承载消息业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMessageDatabaseService implements MessageDatabaseService {

    private static final String STATUS_SENT = "sent";

    private final MessageMapper messageMapper;

    public MybatisPlusMessageDatabaseService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 插入新消息记录。
     */
    @Override
    public void insert(MessageRecord record) {
        messageMapper.insert(MessageEntity.from(record));
    }

    /**
     * 按消息 ID 查询记录。
     */
    @Override
    public Optional<MessageRecord> findById(long messageId) {
        return Optional.ofNullable(messageMapper.selectById(messageId))
                .map(MessageEntity::toRecord);
    }

    /**
     * 更新既有消息记录。
     * 边界：只更新 domain 载荷、提及、摘要与状态四列，不改写发送者与频道归属。
     */
    @Override
    public void update(MessageRecord record) {
        messageMapper.update(null, new LambdaUpdateWrapper<MessageEntity>()
                .eq(MessageEntity::getMessageId, record.messageId())
                .set(MessageEntity::getData, record.data())
                .set(MessageEntity::getMentions, record.mentions())
                .set(MessageEntity::getPreview, record.preview())
                .set(MessageEntity::getStatus, record.status()));
    }

    /**
     * 查询频道内早于游标的消息记录。
     * 输出：按消息 ID 倒序返回，最多 limit 条。
     */
    @Override
    public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getChannelId, channelId)
                        .lt(cursorMessageId != null, MessageEntity::getMessageId, cursorMessageId)
                        .orderByDesc(MessageEntity::getMessageId)
                        .last("LIMIT " + limit))
                .stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    /**
     * 查询频道内晚于锚点的消息记录。
     * 输出：按消息 ID 升序返回，最多 limit 条。
     */
    @Override
    public List<MessageRecord> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getChannelId, channelId)
                        .gt(MessageEntity::getMessageId, afterMessageId)
                        .orderByAsc(MessageEntity::getMessageId)
                        .last("LIMIT " + limit))
                .stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    /**
     * 按复合过滤条件搜索频道消息。
     * 输入：关键字、发送者、领域类型、游标和前后消息锚点。
     */
    @Override
    public List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            Long beforeMessageId,
            Long afterMessageId,
            int limit
    ) {
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getChannelId, channelId)
                        .eq(MessageEntity::getStatus, STATUS_SENT)
                        .and(wrapper -> wrapper
                                .like(MessageEntity::getPreview, keyword)
                                .or()
                                .apply("CAST(data AS CHAR) LIKE CONCAT('%', {0}, '%')", keyword))
                        .lt(cursorMessageId != null, MessageEntity::getMessageId, cursorMessageId)
                        .eq(senderAccountId != null, MessageEntity::getSenderId, senderAccountId)
                        .eq(domain != null && !domain.isBlank(), MessageEntity::getDomain, domain)
                        .lt(beforeMessageId != null, MessageEntity::getMessageId, beforeMessageId)
                        .gt(afterMessageId != null, MessageEntity::getMessageId, afterMessageId)
                        .orderByDesc(MessageEntity::getMessageId)
                        .last("LIMIT " + limit))
                .stream()
                .map(MessageEntity::toRecord)
                .toList();
    }
}
