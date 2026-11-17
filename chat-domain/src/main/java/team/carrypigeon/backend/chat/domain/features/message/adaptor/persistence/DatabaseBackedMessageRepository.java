package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageSendRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureErrorCode;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPMessageStatus;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 canonical 领域消息与数据库 JSON 投影之间转换。
 * 边界：不解释具体 domain 的 data，也不恢复任何旧消息字段。
 */
@Component
public class DatabaseBackedMessageRepository implements MessageRepository {

    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<Long>> MENTIONS_TYPE = new TypeReference<>() { };
    private static final String STATUS_SENT = "sent";

    private final MessageDatabaseService messageDatabaseService;
    private final JsonProviderImpl jsonProvider;
    private final ObjectReader dataReader;

    public DatabaseBackedMessageRepository(
            MessageDatabaseService messageDatabaseService, JsonProviderImpl jsonProvider, ObjectMapper objectMapper
    ) {
        this.messageDatabaseService = messageDatabaseService;
        this.jsonProvider = jsonProvider;
        // 无类型 Map 的整数统一恢复为 Long，避免小 ID 被 Jackson 恢复为 Integer。
        this.dataReader = objectMapper.readerFor(DATA_TYPE).with(DeserializationFeature.USE_LONG_FOR_INTS);
    }

    @Override
    public boolean deleteChannelBatch(long channelId, int limit) {
        return messageDatabaseService.deleteChannelBatch(channelId, limit);
    }

    @Override
    public MessageSendRequest reserveSend(long accountId, long channelId, UUID requestId, String payloadHash, long proposedMessageId) {
        var record = messageDatabaseService.reserveSend(accountId, channelId, requestId, payloadHash, proposedMessageId);
        return new MessageSendRequest(record.payloadHash(), record.messageId());
    }

    @Override
    public long nextSequence(long channelId) {
        return messageDatabaseService.nextSequence(channelId);
    }

    @Override
    public void save(CPChannelMessage message) {
        messageDatabaseService.insert(toPersistenceRecord(message));
    }

    @Override
    public java.util.Optional<CPChannelMessage> findById(long messageId) {
        return messageDatabaseService.findById(messageId).map(this::toDomainMessage);
    }

    @Override
    public java.util.Optional<CPChannelMessage> findByIdForUpdate(long messageId) {
        return messageDatabaseService.findByIdForUpdate(messageId).map(this::toDomainMessage);
    }

    @Override
    public void update(CPChannelMessage message) {
        messageDatabaseService.update(toPersistenceRecord(message));
    }

    @Override
    public List<CPChannelMessage> findLatestByChannelId(long channelId, int limit) {
        return messageDatabaseService
            .findLatestByChannelId(channelId, limit)
            .stream()
            .map(this::toDomainMessage).toList();
    }

    @Override
    public List<CPChannelMessage> findBeforeByChannelId(long channelId, long cursorSequence, int limit) {
        return messageDatabaseService
            .findBeforeByChannelId(channelId, cursorSequence, limit)
            .stream()
            .map(this::toDomainMessage).toList();
    }

    @Override
    public List<CPChannelMessage> findAfterByChannelId(long channelId, long cursorSequence, int limit) {
        return messageDatabaseService
            .findAfterByChannelId(channelId, cursorSequence, limit)
            .stream()
            .map(this::toDomainMessage)
            .toList();
    }

    @Override
    public List<CPChannelMessage> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorSequence,
            Long senderAccountId,
            String domain,
            int limit
    ) {
        return messageDatabaseService
            .searchByChannelId(channelId, keyword, cursorSequence, senderAccountId, domain, STATUS_SENT, limit)
            .stream()
            .map(this::toDomainMessage)
            .toList();
    }

    private CPChannelMessage toDomainMessage(MessageRecord record) {
        return new CPChannelMessage(
                record.messageId(),
                record.senderId(),
                record.channelId(),
                record.sequence(),
                record.domain(),
                parseData(record.data()),
                record.sendTime(),
                parseMentions(record.mentions()),
                record.preview(),
                record.searchText(),
                CPMessageStatus.valueOf(record.status().toUpperCase(java.util.Locale.ROOT))
        );
    }

    private MessageRecord toPersistenceRecord(CPChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.sequence(),
                message.domain(),
                jsonProvider.toJson(message.data()),
                message.sendTime(),
                jsonProvider.toJson(message.mentions()),
                message.preview(),
                message.searchText(),
                message.status().name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    private Map<String, Object> parseData(String data) {
        if (data == null || data.isBlank()) {
            return Map.of();
        }
        try {
            return dataReader.readValue(data);
        } catch (JsonProcessingException exception) {
            throw new InfrastructureException(
                InfrastructureErrorCode.JSON_DESERIALIZE_FAILED, "Failed to deserialize message data", exception
            );
        }
    }

    private List<Long> parseMentions(String mentions) {
        return mentions == null || mentions.isBlank() ? List.of() : jsonProvider.fromJson(mentions, MENTIONS_TYPE);
    }
}
