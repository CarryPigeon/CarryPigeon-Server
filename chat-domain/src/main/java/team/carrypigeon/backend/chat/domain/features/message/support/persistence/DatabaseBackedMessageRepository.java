package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 message feature 内完成领域消息与数据库契约模型之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedMessageRepository implements MessageRepository {

    private final MessageDatabaseService messageDatabaseService;

    public DatabaseBackedMessageRepository(MessageDatabaseService messageDatabaseService) {
        this.messageDatabaseService = messageDatabaseService;
    }

    @Override
    public ChannelMessage save(ChannelMessage message) {
        messageDatabaseService.insert(toRecord(message));
        return message;
    }

    @Override
    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return messageDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }

    private ChannelMessage toDomainModel(MessageRecord record) {
        return new ChannelMessage(
                record.messageId(),
                record.serverId(),
                record.conversationId(),
                record.channelId(),
                record.senderId(),
                record.messageType(),
                record.content(),
                record.payload(),
                record.metadata(),
                record.status(),
                record.createdAt()
        );
    }

    private MessageRecord toRecord(ChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.content(),
                message.payload(),
                message.metadata(),
                message.status(),
                message.createdAt()
        );
    }
}
