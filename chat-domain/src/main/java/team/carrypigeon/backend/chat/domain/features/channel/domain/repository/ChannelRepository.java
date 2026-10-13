package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;

/**
 * 频道仓储抽象。
 * 职责：定义频道聚合的业务语义查询入口。
 * 边界：不暴露数据库实现细节。
 */
public interface ChannelRepository {

    /**
     * 查询 system 频道。
     *
     * @return 返回系统频道
     */
    List<Channel> findSystemChannels();

    /**
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道，未命中时返回空
     */
    Optional<Channel> findById(long channelId);

    /**
     * 通过id组查询频道
     *
     * @param channelIds 频道id组
     * @return 频道组
     * */
    List<Channel> findByIds(Collection<Long> channelIds);

    /**
     * 按发现页规则查询频道集合。
     * 输入：关键字、游标、类型与查询上限。
     *
     * @param keyword 关键字
     * @param cursorChannelId 游标频道 ID
     * @param limit 查询上限
     * @return 频道集合
     */
    List<DiscoveredChannel> discoverChannels(String keyword, Long cursorChannelId, int limit);

    /**
     * 保存频道。
     *
     * @param channel 待保存频道
     */
     void save(Channel channel);
    /**
     * 更新频道。
     *
     * @param channel 待更新频道
     */
    void update(Channel channel);

    /**
     * 删除频道。
     *
     * @param channelId 频道 ID
     */
    void delete(long channelId);
}
