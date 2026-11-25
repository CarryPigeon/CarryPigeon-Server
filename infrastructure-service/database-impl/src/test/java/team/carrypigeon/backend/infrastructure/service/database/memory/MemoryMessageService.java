package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/** Message 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryMessageService extends LongIdTable<MessageRecord> implements MessageDatabaseService {

    public MemoryMessageService(MemoryDatabase database) {
        super(database, MessageRecord::messageId);
    }

    @Override
    public Optional<MessageRecord> findByIdForUpdate(long messageId) {
        return findById(messageId);
    }

    @Override
    public List<MessageRecord> findLatestByChannelId(long channelId, int limit) {
        return select(r -> r.channelId() == channelId).reversed().stream().limit(limit).toList();
    }

    @Override
    public List<MessageRecord> findBeforeByChannelId(long channelId, long cursorMessageId, int limit) {
        return select(r -> r.channelId() == channelId && r.messageId() < cursorMessageId).reversed().stream().limit(limit).toList();
    }

    @Override
    public List<MessageRecord> findAfterByChannelId(long channelId, long cursorMessageId, int limit) {
        return select(r -> r.channelId() == channelId && r.messageId() > cursorMessageId).stream().limit(limit).toList();
    }

    @Override
    public List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            String status,
            int limit
    ) {
        // 测试替身按正文子串匹配；MySQL ngram 分词效果需在真实后端验证。
        return select(r -> r.channelId() == channelId && r.searchText().contains(keyword)
                && (cursorMessageId == null || r.messageId() < cursorMessageId)
                && (senderAccountId == null || r.senderId() == senderAccountId)
                && (domain == null || domain.isBlank() || r.domain().equals(domain))
                && (status == null || status.isBlank() || r.status().equals(status)))
                .reversed().stream().limit(limit).toList();
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }

    @Override
    public Long findLatestMessageIdForJoin(long channelId) {
        return findLatestByChannelId(channelId, 1).stream().map(MessageRecord::messageId).findFirst().orElse(null);
    }
}
