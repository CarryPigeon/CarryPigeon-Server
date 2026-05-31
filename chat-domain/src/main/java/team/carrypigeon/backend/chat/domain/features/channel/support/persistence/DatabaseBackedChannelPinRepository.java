package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelPinDatabaseService;

/**
 * 基于 database-api 的频道置顶仓储适配器。
 */
public class DatabaseBackedChannelPinRepository implements ChannelPinRepository {

    private final ChannelPinDatabaseService channelPinDatabaseService;

    public DatabaseBackedChannelPinRepository(ChannelPinDatabaseService channelPinDatabaseService) {
        this.channelPinDatabaseService = channelPinDatabaseService;
    }

    @Override
    public Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
        return channelPinDatabaseService.findByChannelIdAndMessageId(channelId, messageId).map(this::toDomain);
    }

    @Override
    public void save(ChannelPin channelPin) {
        channelPinDatabaseService.insert(toRecord(channelPin));
    }

    @Override
    public void delete(long channelId, long messageId) {
        channelPinDatabaseService.delete(channelId, messageId);
    }

    @Override
    public List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return channelPinDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByChannelId(long channelId) {
        return channelPinDatabaseService.countByChannelId(channelId);
    }

    private ChannelPin toDomain(ChannelPinRecord record) {
        return new ChannelPin(record.pinId(), record.channelId(), record.messageId(), record.pinnedByAccountId(), record.note(), record.pinnedAt());
    }

    private ChannelPinRecord toRecord(ChannelPin channelPin) {
        return new ChannelPinRecord(channelPin.pinId(), channelPin.channelId(), channelPin.messageId(), channelPin.pinnedByAccountId(), channelPin.note(), channelPin.pinnedAt());
    }
}
