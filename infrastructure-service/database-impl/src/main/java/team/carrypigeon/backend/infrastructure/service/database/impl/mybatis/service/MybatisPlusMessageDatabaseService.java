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
 * 职责：在 database-impl 中完成消息最小读写能力。
 * 边界：只负责数据库记录映射，不承载消息业务规则。
 */
public class MybatisPlusMessageDatabaseService implements MessageDatabaseService {

    private final MessageMapper messageMapper;

    public MybatisPlusMessageDatabaseService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void insert(MessageRecord record) {
        try {
            messageMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert message", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert message", exception);
        }
    }

    @Override
    public java.util.Optional<MessageRecord> findById(long messageId) {
        try {
            return java.util.Optional.ofNullable(messageMapper.findById(messageId))
                    .map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query message", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query message", exception);
        }
    }

    @Override
    public void update(MessageRecord record) {
        try {
            messageMapper.updateMessage(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to update message", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to update message", exception);
        }
    }

    @Override
    public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        try {
            return messageMapper.findByChannelIdBefore(channelId, cursorMessageId, limit)
                    .stream()
                    .map(this::toRecord)
                    .toList();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel messages", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query channel messages", exception);
        }
    }

    @Override
    public List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit) {
        try {
            return messageMapper.searchByChannelId(channelId, keyword, limit)
                    .stream()
                    .map(this::toRecord)
                    .toList();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to search channel messages", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to search channel messages", exception);
        }
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
                entity.getStatus(),
                entity.getCreatedAt()
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
        entity.setStatus(record.status());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }
}
