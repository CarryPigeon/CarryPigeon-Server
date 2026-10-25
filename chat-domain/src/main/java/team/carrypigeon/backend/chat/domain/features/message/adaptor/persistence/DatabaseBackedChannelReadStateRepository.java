package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * 职责：在 message feature 内完成已读状态领域模型与 database-api 记录模型转换，并编排“只前进不后退”规则。
 * 边界：不解释未读统计 SQL；该能力属于 database 读模型例外。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelReadStateRepository implements ChannelReadStateRepository {

    private final ChannelReadStateDatabaseService channelReadStateDatabaseService;

    public DatabaseBackedChannelReadStateRepository(ChannelReadStateDatabaseService channelReadStateDatabaseService) {
        this.channelReadStateDatabaseService = channelReadStateDatabaseService;
    }

    /**
     * 查询账户在频道中的已读状态。
     */
    @Override
    public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
        return channelReadStateDatabaseService.findById(new ChannelReadStateKey(channelId, accountId)).map(this::toDomain);
    }

    /**
     * 新增或覆盖已读状态。
     * 输出：返回调用方传入的领域对象，保持上层事务上下文一致。
     */
    @Override
    public ChannelReadState upsert(ChannelReadState readState) {
        channelReadStateDatabaseService.upsert(toRecord(readState));
        return readState;
    }

    @Override
    public boolean advanceIfNewer(ChannelReadState readState) {
        Optional<ChannelReadState> current = channelReadStateDatabaseService
                .findByIdForUpdate(new ChannelReadStateKey(readState.channelId(), readState.accountId()))
                .map(this::toDomain);
        if (current.isPresent() && current.get().lastReadMessageId() >= readState.lastReadMessageId()) {
            return false;
        }
        channelReadStateDatabaseService.upsert(toRecord(readState));
        return true;
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
