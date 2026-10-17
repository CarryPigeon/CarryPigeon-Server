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
 * 职责：把 {@link ChannelRepository} 的领域语义调用翻译为 database-api 的最小数据库契约，并在两侧模型之间做纯转换。
 * 边界：不含 SQL 与数据库驱动细节，不承载频道可见性、权限与资料校验规则；事务边界由调用方（领域服务）决定。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelRepository implements ChannelRepository {

    private final ChannelDatabaseService channelDatabaseService;

    public DatabaseBackedChannelRepository(ChannelDatabaseService channelDatabaseService) {
        this.channelDatabaseService = channelDatabaseService;
    }

    @Override
    public List<Channel> listSystemChannels() {
        return channelDatabaseService.findSystemChannels().stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    public Optional<Channel> findById(long channelId) {
        return channelDatabaseService.findById(channelId).map(this::toDomainModel);
    }

    @Override
    public List<Channel> listByIds(Collection<Long> channelIds) {
        // 注意：本适配器不处理空集合，调用方需自行拦截，否则会下传空 IN 条件
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

    /**
     * 把频道持久化契约转换为频道领域模型。
     * 说明：两侧字段一一对应，仅做类型与命名归一，不注入任何业务规则。
     *
     * @param record 频道持久化投影
     * @return 频道领域模型
     */
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

    /**
     * 把频道发现持久化投影转换为发现读模型。
     * 说明：该投影只服务发现读链路，成员数与加入门槛提示均来自数据库侧聚合结果，本方法不再补算。
     *
     * @param record 频道发现持久化投影
     * @return 频道发现读模型
     */
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

    /**
     * 把频道领域模型转换为持久化契约。
     * 说明：会连同 systemChannel 一起写入，因此调用方必须在更新前保留该标记，否则会把系统频道降级为普通频道。
     *
     * @param channel 频道领域模型
     * @return 频道持久化投影
     */
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
