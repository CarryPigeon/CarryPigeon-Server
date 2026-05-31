package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;

/**
 * 基于 database-api 的频道已读状态仓储适配器。
 */
public class DatabaseBackedChannelReadStateRepository implements ChannelReadStateRepository {

    private final ChannelReadStateDatabaseService channelReadStateDatabaseService;

    public DatabaseBackedChannelReadStateRepository(ChannelReadStateDatabaseService channelReadStateDatabaseService) {
        this.channelReadStateDatabaseService = channelReadStateDatabaseService;
    }

    @Override
    public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
        return channelReadStateDatabaseService.findByChannelIdAndAccountId(channelId, accountId).map(this::toDomain);
    }

    @Override
    public ChannelReadState upsert(ChannelReadState readState) {
        channelReadStateDatabaseService.upsert(toRecord(readState));
        return readState;
    }

    @Override
    public List<ChannelUnread> listUnreadsByAccountId(long accountId) {
        return channelReadStateDatabaseService.listUnreadsByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ChannelReadState toDomain(ChannelReadStateRecord record) {
        return new ChannelReadState(
                record.channelId(),
                record.accountId(),
                record.lastReadMessageId(),
                record.lastReadTime(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private ChannelReadStateRecord toRecord(ChannelReadState readState) {
        return new ChannelReadStateRecord(
                readState.channelId(),
                readState.accountId(),
                readState.lastReadMessageId(),
                readState.lastReadTime(),
                readState.createdAt(),
                readState.updatedAt()
        );
    }

    private ChannelUnread toDomain(ChannelUnreadRecord record) {
        return new ChannelUnread(record.channelId(), record.unreadCount(), record.lastReadTime());
    }
}
