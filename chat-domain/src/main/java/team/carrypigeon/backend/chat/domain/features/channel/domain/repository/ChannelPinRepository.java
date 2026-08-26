package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;

/**
 * 频道置顶仓储抽象。
 */
public interface ChannelPinRepository {

    Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId);

    void save(ChannelPin channelPin);

    void delete(long channelId, long messageId);

    List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    long countByChannelId(long channelId);
}
