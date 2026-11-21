package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;

/**
 * 基于 database-api 的频道已读状态仓储适配器。
 * 职责：在 message feature 内完成已读状态领域模型与 database-api 记录模型转换，并提供写事务内的锁定读。
 * 边界：不解释未读统计 SQL；该能力属于 database 读模型例外。
 */
@Component
public class DatabaseBackedChannelReadStateRepository implements ChannelReadStateRepository {

    private final ChannelReadStateDatabaseService channelReadStateDatabaseService;

    public DatabaseBackedChannelReadStateRepository(ChannelReadStateDatabaseService channelReadStateDatabaseService) {
        this.channelReadStateDatabaseService = channelReadStateDatabaseService;
    }

    @Override
    public void deleteByChannelId(long channelId) {
        channelReadStateDatabaseService.deleteByChannelId(channelId);
    }

    /**
     * 查询账户在频道中的已读状态。
     */
    @Override
    public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
        return channelReadStateDatabaseService.findById(new ChannelReadStateKey(channelId, accountId)).map(this::toDomain);
    }

    @Override
    public Optional<ChannelReadState> findForUpdate(long channelId, long accountId) {
        return channelReadStateDatabaseService.findByIdForUpdate(new ChannelReadStateKey(channelId, accountId))
                .map(this::toDomain);
    }

    @Override
    public void save(ChannelReadState readState) {
        channelReadStateDatabaseService.upsert(toRecord(readState));
    }

    /**
     * 查询账户各频道的未读统计投影。
     */
    @Override
    public List<ChannelUnread> listUnreadsByAccountId(long accountId) {
        return channelReadStateDatabaseService.findUnreadsByAccountId(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ChannelReadState toDomain(ChannelReadStateRecord record) {
        return new ChannelReadState(
                record.channelId(),
                record.accountId(),
                record.lastReadMessageId(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private ChannelReadStateRecord toRecord(ChannelReadState readState) {
        return new ChannelReadStateRecord(
                readState.channelId(),
                readState.accountId(),
                readState.lastReadMessageId(),
                readState.createdAt(),
                readState.updatedAt()
        );
    }

    private ChannelUnread toDomain(ChannelUnreadRecord record) {
        return new ChannelUnread(record.channelId(), record.unreadCount());
    }

    @Override
    public long countUnreads(long channelId, long accountId, long afterMessageId) {
        return channelReadStateDatabaseService.countUnreads(channelId, accountId, afterMessageId);
    }
}
