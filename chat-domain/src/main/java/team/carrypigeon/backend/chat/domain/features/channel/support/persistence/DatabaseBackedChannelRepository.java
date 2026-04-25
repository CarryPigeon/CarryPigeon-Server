package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;

/**
 * 基于 database-api 的频道仓储适配器。
 * 职责：在 channel feature 内完成领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedChannelRepository implements ChannelRepository {

    private final ChannelDatabaseService channelDatabaseService;

    public DatabaseBackedChannelRepository(ChannelDatabaseService channelDatabaseService) {
        this.channelDatabaseService = channelDatabaseService;
    }

    @Override
    public Optional<Channel> findDefaultChannel() {
        return channelDatabaseService.findDefaultChannel().map(this::toDomainModel);
    }

    @Override
    public Optional<Channel> findSystemChannel() {
        return channelDatabaseService.findSystemChannel().map(this::toDomainModel);
    }

    @Override
    public Optional<Channel> findById(long channelId) {
        return channelDatabaseService.findById(channelId).map(this::toDomainModel);
    }

    @Override
    public Channel save(Channel channel) {
        channelDatabaseService.insert(toRecord(channel));
        return channel;
    }

    private Channel toDomainModel(ChannelRecord record) {
        return new Channel(
                record.id(),
                record.conversationId(),
                record.name(),
                record.type(),
                record.defaultChannel(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private ChannelRecord toRecord(Channel channel) {
        return new ChannelRecord(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
