package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;

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
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道，未命中时返回空
     */
    Optional<Channel> findById(long channelId);

    /**
     * 保存频道。
     *
     * @param channel 待保存频道
     * @return 已保存频道
     */
    default Channel save(Channel channel) {
        throw new UnsupportedOperationException("channel save is not supported");
    }
}
