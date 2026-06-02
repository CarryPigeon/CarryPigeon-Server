package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

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
     * 查询默认公共频道。
     *
     * @return 命中时返回频道，未命中时返回空
     */
    Optional<Channel> findDefaultChannel();

    /**
     * 查询 system 频道。
     *
     * @return 命中时返回 system 频道，未命中时返回空
     */
    Optional<Channel> findSystemChannel();

    /**
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道，未命中时返回空
     */
    Optional<Channel> findById(long channelId);

    default List<DiscoveredChannel> discoverChannels(String keyword, Long cursorChannelId, String type, int limit) {
        throw new UnsupportedOperationException("channel discover is not supported");
    }

    /**
     * 保存频道。
     *
     * @param channel 待保存频道
     * @return 已保存频道
     */
    default Channel save(Channel channel) {
        throw new UnsupportedOperationException("channel save is not supported");
    }

    /**
     * 更新频道。
     *
     * @param channel 待更新频道
     * @return 已更新频道
     */
    default Channel update(Channel channel) {
        throw new UnsupportedOperationException("channel update is not supported");
    }

    /**
     * 删除频道。
     *
     * @param channelId 频道 ID
     */
    default void delete(long channelId) {
        throw new UnsupportedOperationException("channel delete is not supported");
    }
}
