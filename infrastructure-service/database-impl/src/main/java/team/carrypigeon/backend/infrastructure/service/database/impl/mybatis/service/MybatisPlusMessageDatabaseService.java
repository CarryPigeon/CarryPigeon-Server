package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;

/**
 * MyBatis-Plus 消息数据库服务。
 * 职责：在 database-impl 中完成消息表级读写与专用搜索查询。
 * 边界：只负责持久化映射和数据谓词，不承载消息业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMessageDatabaseService implements MessageDatabaseService {

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
     * 批量插入消息记录。
     */
    @Override
    public void insertAll(List<MessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        messageMapper.insertAll(records.stream().map(MessageEntity::from).toList());
    }

    /**
     * 按消息 ID 删除记录。
     */
    @Override
    public void delete(long messageId) {
        messageMapper.deleteById(messageId);
    }

    /**
     * 按消息 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageMapper.deleteByIds(messageIds);
    }

    /**
     * 查询消息表全部记录。
     */
    @Override
    public List<MessageRecord> list() {
        return messageMapper.selectList(null).stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    /**
     * 按消息 ID 批量查询记录。
     */
    @Override
    public List<MessageRecord> findByIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return messageMapper.selectByIds(messageIds).stream()
                .map(MessageEntity::toRecord)
                .toList();
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
     * 更新完整消息记录。
     * 边界：字段变化由 Repository 决定，本层不筛选列。
     */
    @Override
    public void update(MessageRecord record) {
        messageMapper.updateById(MessageEntity.from(record));
    }

    /**
     * 按通用条件查询消息记录。
     */
    @Override
    public List<MessageRecord> find(MessageQuery query) {
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, query.channelId())
                .lt(query.beforeMessageId() != null, MessageEntity::getMessageId, query.beforeMessageId())
                .gt(query.afterMessageId() != null, MessageEntity::getMessageId, query.afterMessageId())
                .eq(query.senderAccountId() != null, MessageEntity::getSenderId, query.senderAccountId())
                .eq(query.domain() != null && !query.domain().isBlank(), MessageEntity::getDomain, query.domain())
                .eq(query.status() != null && !query.status().isBlank(), MessageEntity::getStatus, query.status());
        if (query.ascending()) {
            wrapper.orderByAsc(MessageEntity::getMessageId);
        } else {
            wrapper.orderByDesc(MessageEntity::getMessageId);
        }
        wrapper.last("LIMIT " + query.limit());
        return messageMapper.selectList(wrapper).stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    /**
     * 按复合过滤条件搜索频道消息。
     * 说明：搜索是专用查询；status 由 Repository 传入，不在本层硬编码。
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
            String status,
            int limit
    ) {
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getChannelId, channelId)
                        .eq(status != null && !status.isBlank(), MessageEntity::getStatus, status)
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
