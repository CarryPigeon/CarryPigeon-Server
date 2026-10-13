package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;

/**
 * 基于 database-api 的频道仓储适配器。
 * 职责：在 channel feature 内完成领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelRepository implements ChannelRepository {

    private final ChannelDatabaseService channelDatabaseService;

    public DatabaseBackedChannelRepository(ChannelDatabaseService channelDatabaseService) {
        this.channelDatabaseService = channelDatabaseService;
    }

    @Override
    public List<Channel> findSystemChannels() {
        return channelDatabaseService.findSystemChannels().stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    public Optional<Channel> findById(long channelId) {
        return channelDatabaseService.findById(channelId).map(this::toDomainModel);
    }

    @Override
    public List<Channel> findByIds(Collection<Long> channelIds) {
        return channelDatabaseService.findByIds(channelIds).stream()
                .map(this::toDomainModel)
                .toList();
    }

    /**
     * 按发现页规则查询频道集合。
     * 输入：关键字、游标、类型与查询上限。
     */
    @Override
    public List<DiscoveredChannel> discoverChannels(String keyword, Long cursorChannelId, int limit) {
        return channelDatabaseService.discoverChannels(keyword, cursorChannelId, limit).stream()
                .map(this::toDiscoveredChannel)
                .toList();
    }

    @Override
    public void save(Channel channel) {
        channelDatabaseService.insert(toRecord(channel));
    }

    @Override
    public void update(Channel channel) {
        channelDatabaseService.update(toRecord(channel));
    }

    @Override
    public void delete(long channelId) {
        channelDatabaseService.delete(channelId);
    }


    private Channel toDomainModel(ChannelRecord record) {
        return new Channel(
                record.id(),
                record.name(),
                record.brief(),
                record.avatar(),
                record.systemChannel(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private DiscoveredChannel toDiscoveredChannel(ChannelDiscoverRecord record) {
        return new DiscoveredChannel(
                record.id(),
                record.name(),
                record.brief(),
                record.avatar(),
                record.memberCount(),
                record.requiresApplication()
        );
    }

    private ChannelRecord toRecord(Channel channel) {
        return new ChannelRecord(
                channel.id(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                channel.systemChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
