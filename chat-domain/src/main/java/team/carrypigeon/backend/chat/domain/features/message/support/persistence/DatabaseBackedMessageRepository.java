package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 canonical 领域消息与数据库 JSON 投影之间转换。
 * 边界：不解释具体 domain 的 data，也不恢复任何旧消息字段。
 */
public class DatabaseBackedMessageRepository implements MessageRepository {

    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<Long>> MENTIONS_TYPE = new TypeReference<>() { };

    private final MessageDatabaseService messageDatabaseService;
    private final JsonProvider jsonProvider;

    public DatabaseBackedMessageRepository(MessageDatabaseService messageDatabaseService, JsonProvider jsonProvider) {
        this.messageDatabaseService = messageDatabaseService;
        this.jsonProvider = jsonProvider;
    }

    @Override
    public ChannelMessage save(ChannelMessage message) {
        messageDatabaseService.insert(toPersistenceRecord(message));
        return message;
    }

    @Override
    public java.util.Optional<ChannelMessage> findById(long messageId) {
        return messageDatabaseService.findById(messageId).map(this::toDomainMessage);
    }

    @Override
    public ChannelMessage update(ChannelMessage message) {
        messageDatabaseService.update(toPersistenceRecord(message));
        return message;
    }

    @Override
    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return map(messageDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit));
    }

    @Override
    public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return map(messageDatabaseService.findByChannelIdAfter(channelId, afterMessageId, limit));
    }

    @Override
    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
        return map(messageDatabaseService.searchByChannelId(channelId, keyword, limit));
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
        return map(messageDatabaseService.searchByChannelId(
                channelId, keyword, cursorMessageId, senderAccountId, domain,
                beforeMessageId, afterMessageId, limit
        ));
    }

    private List<ChannelMessage> map(List<MessageRecord> records) {
        return records.stream().map(this::toDomainMessage).toList();
    }

    private ChannelMessage toDomainMessage(MessageRecord record) {
        return new ChannelMessage(
                record.messageId(),
                record.senderId(),
                record.channelId(),
                record.domain(),
                record.domainVersion(),
                parseData(record.data()),
                record.sendTime(),
                parseMentions(record.mentions()),
                record.preview(),
                MessageStatus.valueOf(record.status().toUpperCase(java.util.Locale.ROOT))
        );
    }

    private MessageRecord toPersistenceRecord(ChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                jsonProvider.toJson(message.data()),
                message.sendTime(),
                jsonProvider.toJson(message.mentions().stream().map(String::valueOf).toList()),
                message.preview(),
                message.status().name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    private Map<String, Object> parseData(String data) {
        return data == null || data.isBlank() ? Map.of() : jsonProvider.fromJson(data, DATA_TYPE);
    }

    private List<Long> parseMentions(String mentions) {
        return mentions == null || mentions.isBlank() ? List.of() : jsonProvider.fromJson(mentions, MENTIONS_TYPE);
    }
}
