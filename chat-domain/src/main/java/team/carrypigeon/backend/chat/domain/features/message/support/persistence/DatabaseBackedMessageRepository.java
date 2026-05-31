package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 message feature 内完成领域消息与数据库持久化投影之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，也不把 database-api 契约直接提升为领域模型。
 */
public class DatabaseBackedMessageRepository implements MessageRepository {

    private final MessageDatabaseService messageDatabaseService;

    public DatabaseBackedMessageRepository(MessageDatabaseService messageDatabaseService) {
        this.messageDatabaseService = messageDatabaseService;
    }

    @Override
    public ChannelMessage save(ChannelMessage message) {
        messageDatabaseService.insert(toPersistenceRecord(message));
        return message;
    }

    @Override
    public java.util.Optional<ChannelMessage> findById(long messageId) {
        return messageDatabaseService.findById(messageId)
                .map(this::toDomainMessage);
    }

    @Override
    public ChannelMessage update(ChannelMessage message) {
        messageDatabaseService.update(toPersistenceRecord(message));
        return message;
    }

    @Override
    public void delete(long messageId) {
        messageDatabaseService.delete(messageId);
    }

    @Override
    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return messageDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    @Override
    public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return messageDatabaseService.findByChannelIdAfter(channelId, afterMessageId, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    @Override
    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
        return messageDatabaseService.searchByChannelId(channelId, keyword, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    @Override
    public List<ChannelMessage> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            Long beforeMessageId,
            Long afterMessageId,
            int limit
    ) {
        return messageDatabaseService.searchByChannelId(
                        channelId,
                        keyword,
                        cursorMessageId,
                        senderAccountId,
                        domain,
                        beforeMessageId,
                        afterMessageId,
                        limit
                ).stream()
                .map(this::toDomainMessage)
                .toList();
    }

    private ChannelMessage toDomainMessage(MessageRecord record) {
        return new ChannelMessage(
                record.messageId(),
                record.serverId(),
                record.conversationId(),
                record.channelId(),
                record.senderId(),
                record.messageType(),
                record.body(),
                record.previewText(),
                record.searchableText(),
                record.payload(),
                record.metadata(),
                record.mentions(),
                record.forwardedFrom(),
                record.status(),
                record.createdAt(),
                record.editedAt(),
                record.editVersion()
        );
    }

    private MessageRecord toPersistenceRecord(ChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                message.searchableText(),
                message.payload(),
                message.metadata(),
                message.mentions(),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }
}
