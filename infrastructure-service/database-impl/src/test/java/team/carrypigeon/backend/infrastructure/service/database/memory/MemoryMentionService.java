package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.List;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

/** Mention 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryMentionService extends LongIdTable<MentionRecord> implements MentionDatabaseService {

    public MemoryMentionService(MemoryDatabase database) {
        super(database, MentionRecord::mentionId);
    }

    @Override
    public List<MentionRecord> findVisibleByIds(long accountId, List<Long> mentionIds) {
        return select(r -> r.targetAccountId() == accountId && mentionIds.contains(r.mentionId()));
    }

    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return database.locked(() -> {
            return findById(mentionId).filter(r -> r.targetAccountId() == accountId)
                    .map(r -> r.read() || update(asRead(r))).orElse(false);
        });
    }

    @Override
    public List<MentionRecord> find(MentionQuery query) {
        return select(r -> r.targetAccountId() == query.accountId()
                && (query.beforeMentionId() == null || r.mentionId() < query.beforeMentionId())
                && (query.channelId() == null || r.channelId() == query.channelId())
                && (query.read() == null || r.read() == query.read())).reversed().stream().limit(query.limit()).toList();
    }

    @Override
    public int markAllAsRead(long accountId, Long throughMentionId, Long channelId) {
        return database.locked(() -> {
            var unread = select(r -> !r.read() && r.targetAccountId() == accountId
                    && (throughMentionId == null || r.mentionId() <= throughMentionId)
                    && (channelId == null || r.channelId() == channelId));
            unread.forEach(r -> update(asRead(r)));
            return unread.size();
        });
    }

    @Override
    public boolean updateAll(List<MentionRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        return database.runInTransaction(() -> {
            for (MentionRecord record : records) {
                if (!update(record)) {
                    insert(record);
                }
            }
            return true;
        });
    }

    @Override
    public void deleteByMessageId(long messageId) {
        deleteWhere(r -> r.messageId() == messageId);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }

    @Override
    public void deleteByChannelAndRecipient(long channelId, long accountId) {
        deleteWhere(r -> r.channelId() == channelId && r.targetAccountId() == accountId);
    }

    private MentionRecord asRead(MentionRecord r) {
        return new MentionRecord(r.mentionId(), r.channelId(), r.messageId(), r.fromAccountId(), r.targetType(),
                r.targetAccountId(), r.createdAt(), true);
    }
}
