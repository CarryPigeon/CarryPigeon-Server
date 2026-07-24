package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;

/**
 * MyBatis-Plus 消息数据库服务。
 * 职责：在 database-impl 中完成消息持久化投影的最小读写能力。
 * 边界：只负责持久化投影与实体之间的映射，不承载消息业务规则。
 */
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
        executeVoid(() -> {
            messageMapper.insert(toEntity(record));
        }, "failed to insert message");
    }

    /**
     * 按消息 ID 查询记录。
     */
    @Override
    public java.util.Optional<MessageRecord> findById(long messageId) {
        return execute(
                () -> java.util.Optional.ofNullable(messageMapper.selectById(messageId))
                        .map(this::toRecord),
                "failed to query message"
        );
    }

    /**
     * 更新既有消息记录。
     */
    @Override
    public void update(MessageRecord record) {
        executeVoid(() -> {
            messageMapper.updateMessage(toEntity(record));
        }, "failed to update message");
    }

    /**
     * 查询频道内早于游标的消息记录。
     */
    @Override
    public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return execute(
                () -> messageMapper.findByChannelIdBefore(channelId, cursorMessageId, limit)
                        .stream()
                        .map(this::toRecord)
                        .toList(),
                "failed to query channel messages"
        );
    }

    /**
     * 查询频道内晚于锚点的消息记录。
     */
    @Override
    public List<MessageRecord> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return execute(
                () -> messageMapper.findByChannelIdAfter(channelId, afterMessageId, limit)
                        .stream()
                        .map(this::toRecord)
                        .toList(),
                "failed to query channel messages after anchor"
        );
    }

    /**
     * 按关键字搜索频道消息。
     */
    @Override
    public List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit) {
        return execute(
                () -> messageMapper.searchByChannelId(channelId, keyword, limit)
                        .stream()
                        .map(this::toRecord)
                        .toList(),
                "failed to search channel messages"
        );
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
        return execute(
                () -> messageMapper.searchByChannelIdWithFilters(
                                channelId,
                                keyword,
                                cursorMessageId,
                                senderAccountId,
                                domain,
                                beforeMessageId,
                                afterMessageId,
                                limit
                        ).stream()
                        .map(this::toRecord)
                        .toList(),
                "failed to search channel messages"
        );
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }

    private MessageRecord toRecord(MessageEntity entity) {
        return new MessageRecord(
                entity.getMessageId(),
                entity.getSenderId(),
                entity.getChannelId(),
                entity.getDomain(),
                entity.getDomainVersion(),
                entity.getData(),
                entity.getSendTime(),
                entity.getMentions(),
                entity.getPreview(),
                entity.getStatus()
        );
    }

    private MessageEntity toEntity(MessageRecord record) {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(record.messageId());
        entity.setSenderId(record.senderId());
        entity.setChannelId(record.channelId());
        entity.setDomain(record.domain());
        entity.setDomainVersion(record.domainVersion());
        entity.setData(record.data());
        entity.setSendTime(record.sendTime());
        entity.setMentions(record.mentions());
        entity.setPreview(record.preview());
        entity.setStatus(record.status());
        return entity;
    }

    /**
     * 有返回值的数据库访问操作。
     * 职责：让统一异常包装方法接收 mapper 查询或写入返回值。
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T run();
    }

    /**
     * 无返回值的数据库访问操作。
     * 职责：让统一异常包装方法复用同一条数据库异常转换路径。
     */
    @FunctionalInterface
    private interface VoidDatabaseOperation {
        void run();
    }
}
