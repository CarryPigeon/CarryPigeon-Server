package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;

/** ChannelReadState 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryChannelReadStateService extends MemoryTable<ChannelReadStateKey, ChannelReadStateRecord> implements ChannelReadStateDatabaseService {
    private final MemoryChannelService channels;
    private final MemoryChannelMemberService members;
    private final MemoryMessageService messages;

    public MemoryChannelReadStateService(MemoryDatabase database, MemoryChannelService channels, MemoryChannelMemberService members, MemoryMessageService messages) {
        super(database, r -> new ChannelReadStateKey(r.channelId(), r.accountId()),
                Comparator.comparingLong(ChannelReadStateKey::channelId).thenComparingLong(ChannelReadStateKey::accountId));
        this.channels = channels;
        this.members = members;
        this.messages = messages;
    }

    @Override
    public Optional<ChannelReadStateRecord> findById(ChannelReadStateKey key) {
        return byKey(key);
    }

    @Override
    public Optional<ChannelReadStateRecord> findByIdForUpdate(ChannelReadStateKey key) {
        return findById(key);
    }

    @Override
    public List<ChannelUnreadRecord> findUnreadsByAccountId(long accountId) {
        return database.locked(() -> {
            List<ChannelUnreadRecord> result = new ArrayList<>();
            for (long channelId : members.findChannelIdsByAccountId(accountId).stream().sorted().toList()) {
                long boundary = findById(new ChannelReadStateKey(channelId, accountId))
                        .map(ChannelReadStateRecord::lastReadMessageId).orElse(0L);
                long count = countUnreads(channelId, accountId, boundary);
                if (count > 0) {
                    result.add(new ChannelUnreadRecord(channelId, count));
                }
            }
            return result;
        });
    }

    @Override
    public void upsert(ChannelReadStateRecord record) {
        database.locked(() -> {
            var key = new ChannelReadStateKey(record.channelId(), record.accountId());
            rows.compute(key, (ignored, previous) -> previous == null ? record
                    : new ChannelReadStateRecord(record.channelId(), record.accountId(), record.lastReadMessageId(),
                            previous.createdAt(), record.updatedAt()));
            return null;
        });
    }

    @Override
    public boolean deleteById(ChannelReadStateKey key) {
        return deleteKey(key);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }

    @Override
    public long countUnreads(long channelId, long accountId, long afterMessageId) {
        return database.locked(() -> {
            if (members.findByChannelIdAndAccountId(channelId, accountId).isEmpty() || channels.findById(channelId).isEmpty()) {
                return 0L;
            }
            return messages.select(r -> r.channelId() == channelId && r.senderId() != accountId
                    && r.messageId() > afterMessageId && "sent".equals(r.status())).stream().limit(10000).count();
        });
    }
}
