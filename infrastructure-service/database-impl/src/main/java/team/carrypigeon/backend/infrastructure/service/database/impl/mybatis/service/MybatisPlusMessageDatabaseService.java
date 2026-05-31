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

    @Override
    public void insert(MessageRecord record) {
        executeVoid(() -> {
            messageMapper.insert(toEntity(record));
        }, "failed to insert message");
    }

    @Override
    public java.util.Optional<MessageRecord> findById(long messageId) {
        return execute(
                () -> java.util.Optional.ofNullable(messageMapper.selectById(messageId))
                        .map(this::toRecord),
                "failed to query message"
        );
    }

    @Override
    public void update(MessageRecord record) {
        executeVoid(() -> {
            messageMapper.updateMessage(toEntity(record));
        }, "failed to update message");
    }

    @Override
    public void delete(long messageId) {
        executeVoid(() -> messageMapper.deleteById(messageId), "failed to delete message");
    }

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
                entity.getServerId(),
                entity.getConversationId(),
                entity.getChannelId(),
                entity.getSenderId(),
                entity.getMessageType(),
                entity.getBody(),
                entity.getPreviewText(),
                entity.getSearchableText(),
                entity.getPayload(),
                entity.getMetadata(),
                entity.getMentions(),
                entity.getForwardedFrom(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getEditedAt(),
                entity.getEditVersion() == null ? 1L : entity.getEditVersion()
        );
    }

    private MessageEntity toEntity(MessageRecord record) {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(record.messageId());
        entity.setServerId(record.serverId());
        entity.setConversationId(record.conversationId());
        entity.setChannelId(record.channelId());
        entity.setSenderId(record.senderId());
        entity.setMessageType(record.messageType());
        entity.setBody(record.body());
        entity.setPreviewText(record.previewText());
        entity.setSearchableText(record.searchableText());
        entity.setPayload(record.payload());
        entity.setMetadata(record.metadata());
        entity.setMentions(record.mentions());
        entity.setForwardedFrom(record.forwardedFrom());
        entity.setStatus(record.status());
        entity.setCreatedAt(record.createdAt());
        entity.setEditedAt(record.editedAt());
        entity.setEditVersion(record.editVersion());
        return entity;
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {
        void run();
    }
}
