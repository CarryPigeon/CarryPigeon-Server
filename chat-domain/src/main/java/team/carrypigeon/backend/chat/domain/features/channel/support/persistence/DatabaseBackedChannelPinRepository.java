package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelPinDatabaseService;

/**
 * 基于 database-api 的频道置顶仓储适配器。
 * 职责：在 channel feature 内完成置顶领域模型与 database-api 记录模型转换。
 * 边界：不承载置顶数量、权限或广播规则。
 */
public class DatabaseBackedChannelPinRepository implements ChannelPinRepository {

    private final ChannelPinDatabaseService channelPinDatabaseService;

    public DatabaseBackedChannelPinRepository(ChannelPinDatabaseService channelPinDatabaseService) {
        this.channelPinDatabaseService = channelPinDatabaseService;
    }

    /**
     * 查询指定消息的置顶记录。
     */
    @Override
    public Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
        return channelPinDatabaseService.findByChannelIdAndMessageId(channelId, messageId).map(this::toDomain);
    }

    /**
     * 持久化一条新的置顶记录。
     */
    @Override
    public void save(ChannelPin channelPin) {
        channelPinDatabaseService.insert(toRecord(channelPin));
    }

    /**
     * 删除指定消息的置顶记录。
     */
    @Override
    public void delete(long channelId, long messageId) {
        channelPinDatabaseService.delete(channelId, messageId);
    }

    /**
     * 删除指定消息的全部置顶关系。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        channelPinDatabaseService.deleteByMessageId(messageId);
    }

    /**
     * 查询频道内早于游标消息的置顶记录集合。
     */
    @Override
    public List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return channelPinDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 统计频道当前置顶数量。
     */
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
