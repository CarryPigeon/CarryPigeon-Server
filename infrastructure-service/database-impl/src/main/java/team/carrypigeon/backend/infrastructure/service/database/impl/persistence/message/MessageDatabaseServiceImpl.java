package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageSendRequestRecord;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.MessageMapper;

/**
 * MyBatis-Plus 消息数据库服务。
 * 职责：在 database-impl 中完成消息表级读写与专用搜索查询。
 * 边界：只负责持久化映射和数据谓词，不承载消息业务规则。
 */
public class MessageDatabaseServiceImpl implements MessageDatabaseService {

    private final MessageMapper messageMapper;

    public MessageDatabaseServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByChannelId(long channelId) {
        messageMapper.deleteSendRequestsByChannelId(channelId);
        messageMapper.delete(new LambdaQueryWrapper<MessageEntity>().eq(MessageEntity::getChannelId, channelId));
        messageMapper.deleteSequence(channelId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public MessageSendRequestRecord reserveSend(long accountId, long channelId, UUID requestId, String payloadHash, long proposedMessageId) {
        messageMapper.reserveSend(accountId, channelId, requestId.toString(), payloadHash, proposedMessageId);
        return messageMapper.selectSendForUpdate(accountId, channelId, requestId.toString());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long nextSequence(long channelId) {
        // INSERT/UPDATE 获得排他行锁；MANDATORY 防止脱离消息事务后提前释放锁。
        messageMapper.advanceSequence(channelId);
        return messageMapper.selectSequenceForUpdate(channelId);
    }

    @Override
    public boolean insert(MessageRecord record) {
        int affectedRows = messageMapper.insert(MessageEntity.from(record));
        return affectedRows == 1;
    }

    @Override
    public boolean insertAll(List<MessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = messageMapper.insertAll(records.stream().map(MessageEntity::from).toList());
        return affectedRows == records.size();
    }

    @Override
    public boolean deleteById(long messageId) {
        int affectedRows = messageMapper.deleteById(messageId);
        return affectedRows == 1;
    }

    @Override
    public boolean deleteByIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return true;
        }
        int affectedRows = messageMapper.deleteByIds(messageIds);
        return affectedRows > 0;
    }

    @Override
    public PageResult<MessageRecord> page(PageQuery query) {
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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<MessageRecord> findByIdForUpdate(long messageId) {
        return Optional.ofNullable(messageMapper.selectOne(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getMessageId, messageId).last("FOR UPDATE"))).map(MessageEntity::toRecord);
    }

    @Override
    public boolean update(MessageRecord record) {
        int affectedRows = messageMapper.updateById(MessageEntity.from(record));
        return affectedRows == 1;
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
                // ngram 自然语言全文匹配，仅搜索处理器提供的正文；参数绑定不解释用户输入为 SQL。
                .apply("MATCH(search_text) AGAINST({0} IN NATURAL LANGUAGE MODE)", keyword)
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
