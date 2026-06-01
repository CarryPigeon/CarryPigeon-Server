package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
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

    /**
     * 查询默认公共频道。
     * 输出：存在时返回领域频道，不存在时返回空。
     */
    @Override
    public Optional<Channel> findDefaultChannel() {
        return channelDatabaseService.findDefaultChannel().map(this::toDomainModel);
    }

    /**
     * 查询系统频道。
     * 边界：这里只返回持久化存在的系统频道，不判断当前账户可见性。
     */
    @Override
    public Optional<Channel> findSystemChannel() {
        return channelDatabaseService.findSystemChannel().map(this::toDomainModel);
    }

    /**
     * 按频道 ID 查询单个频道。
     */
    @Override
    public Optional<Channel> findById(long channelId) {
        return channelDatabaseService.findById(channelId).map(this::toDomainModel);
    }

    /**
     * 按发现页规则查询频道集合。
     * 输入：关键字、游标、类型与查询上限。
     */
    @Override
    public List<Channel> discoverChannels(String keyword, Long cursorChannelId, String type, int limit) {
        return channelDatabaseService.discoverChannels(keyword, cursorChannelId, type, limit).stream().map(this::toDomainModel).toList();
    }

    /**
     * 写入一个新频道。
     * 输出：返回原领域对象，保持上层事务上下文中的对象一致。
     */
    @Override
    public Channel save(Channel channel) {
        channelDatabaseService.insert(toRecord(channel));
        return channel;
    }

    /**
     * 更新频道的持久化投影。
     */
    @Override
    public Channel update(Channel channel) {
        channelDatabaseService.update(toRecord(channel));
        return channel;
    }

    /**
     * 删除频道记录。
     * 边界：不级联处理成员、消息等其它业务数据。
     */
    @Override
    public void delete(long channelId) {
        channelDatabaseService.delete(channelId);
    }

    private Channel toDomainModel(ChannelRecord record) {
        return new Channel(
                record.id(),
                record.conversationId(),
                record.name(),
                record.brief(),
                record.avatar(),
                "",
                record.type(),
                record.defaultChannel(),
                record.memberCount(),
                record.requiresApplication(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private ChannelRecord toRecord(Channel channel) {
        return new ChannelRecord(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                channel.type(),
                channel.defaultChannel(),
                channel.memberCount(),
                channel.requiresApplication(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
