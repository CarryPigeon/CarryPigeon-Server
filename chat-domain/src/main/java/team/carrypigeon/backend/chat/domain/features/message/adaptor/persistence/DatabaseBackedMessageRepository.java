package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPMessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 canonical 领域消息与数据库 JSON 投影之间转换。
 * 边界：不解释具体 domain 的 data，也不恢复任何旧消息字段。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedMessageRepository implements MessageRepository {

    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<Long>> MENTIONS_TYPE = new TypeReference<>() { };
    private static final String STATUS_SENT = "sent";

    private final MessageDatabaseService messageDatabaseService;
    private final JsonProviderImpl jsonProvider;

    public DatabaseBackedMessageRepository(MessageDatabaseService messageDatabaseService, JsonProviderImpl jsonProvider) {
        this.messageDatabaseService = messageDatabaseService;
        this.jsonProvider = jsonProvider;
    }

    @Override
    public CPChannelMessage save(CPChannelMessage message) {
        messageDatabaseService.insert(toPersistenceRecord(message));
        return message;
    }

    @Override
    public java.util.Optional<CPChannelMessage> findById(long messageId) {
        return messageDatabaseService.findById(messageId).map(this::toDomainMessage);
    }

    @Override
    public CPChannelMessage update(CPChannelMessage message) {
        messageDatabaseService.update(toPersistenceRecord(message));
        return message;
    }

    @Override
    public List<CPChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return map(messageDatabaseService.find(new MessageQuery(
                channelId,
                cursorMessageId,
                null,
                null,
                null,
                null,
                false,
                limit
        )));
    }

    @Override
    public List<CPChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return map(messageDatabaseService.find(new MessageQuery(
                channelId,
                null,
                afterMessageId,
                null,
                null,
                null,
                true,
                limit
        )));
    }

    @Override
    public List<CPChannelMessage> searchByChannelId(
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
                beforeMessageId, afterMessageId, STATUS_SENT, limit
        ));
    }

    private List<CPChannelMessage> map(List<MessageRecord> records) {
        return records.stream().map(this::toDomainMessage).toList();
    }

    private CPChannelMessage toDomainMessage(MessageRecord record) {
        return new CPChannelMessage(
                record.messageId(),
                record.senderId(),
                record.channelId(),
                record.domain(),
                parseData(record.data()),
                record.sendTime(),
                parseMentions(record.mentions()),
                record.preview(),
                CPMessageStatus.valueOf(record.status().toUpperCase(java.util.Locale.ROOT))
        );
    }

    private MessageRecord toPersistenceRecord(CPChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
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
