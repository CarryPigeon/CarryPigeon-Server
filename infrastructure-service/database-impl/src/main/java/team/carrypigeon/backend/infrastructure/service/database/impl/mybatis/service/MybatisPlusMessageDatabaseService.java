package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
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
public class MybatisPlusMessageDatabaseService implements MessageDatabaseService {

    private final MessageMapper messageMapper;

    public MybatisPlusMessageDatabaseService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long nextSequence(long channelId) {
        // INSERT/UPDATE 获得排他行锁；MANDATORY 防止脱离消息事务后提前释放锁。
        messageMapper.advanceSequence(channelId);
        return messageMapper.selectSequenceForUpdate(channelId);
    }

    @Override
    public void insert(MessageRecord record) {
        messageMapper.insert(MessageEntity.from(record));
    }

    @Override
    public void insertAll(List<MessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        messageMapper.insertAll(records.stream().map(MessageEntity::from).toList());
    }

    @Override
    public void delete(long messageId) {
        messageMapper.deleteById(messageId);
    }

    @Override
    public void deleteAll(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageMapper.deleteByIds(messageIds);
    }

    @Override
    public PageResult<MessageRecord> list(PageQuery query) {
        IPage<MessageEntity> result = messageMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<MessageEntity>()
                        .orderByAsc(MessageEntity::getMessageId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(MessageEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    @Override
    public List<MessageRecord> findByIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return messageMapper.selectByIds(messageIds).stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<MessageRecord> findById(long messageId) {
        return Optional.ofNullable(messageMapper.selectById(messageId))
                .map(MessageEntity::toRecord);
    }

    @Override
    public void update(MessageRecord record) {
        messageMapper.updateById(MessageEntity.from(record));
    }

    @Override
    public List<MessageRecord> findLatestByChannelId(long channelId, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .orderByDesc(MessageEntity::getSequence), limit);
    }

    @Override
    public List<MessageRecord> findBeforeByChannelId(long channelId, long cursorSequence, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .lt(MessageEntity::getSequence, cursorSequence)
                .orderByDesc(MessageEntity::getSequence), limit);
    }

    @Override
    public List<MessageRecord> findAfterByChannelId(long channelId, long cursorSequence, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .gt(MessageEntity::getSequence, cursorSequence)
                .orderByAsc(MessageEntity::getSequence), limit);
    }

    @Override
    public List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorSequence,
            Long senderAccountId,
            String domain,
            String status,
            int limit
    ) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .eq(status != null && !status.isBlank(), MessageEntity::getStatus, status)
                .and(wrapper -> wrapper
                        .like(MessageEntity::getPreview, keyword)
                        .or()
                        .apply("CAST(data AS CHAR) LIKE CONCAT('%', {0}, '%')", keyword))
                .lt(cursorSequence != null, MessageEntity::getSequence, cursorSequence)
                .eq(senderAccountId != null, MessageEntity::getSenderId, senderAccountId)
                .eq(domain != null && !domain.isBlank(), MessageEntity::getDomain, domain)
                .orderByDesc(MessageEntity::getSequence), limit);
    }

    private List<MessageRecord> selectLimited(
            LambdaQueryWrapper<MessageEntity> wrapper,
            int limit
    ) {
        IPage<MessageEntity> page = messageMapper.selectPage(
                new Page<>(1, limit, false),
                wrapper
        );
        return page.getRecords().stream()
                .map(MessageEntity::toRecord)
                .toList();
    }
}
