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

    /**
     * 替换置顶并由持久化层原子保证频道上限。
     */
    default boolean replaceWithinLimit(ChannelPin channelPin, long maxPins) {
        Optional<ChannelPin> existing = findByChannelIdAndMessageId(channelPin.channelId(), channelPin.messageId());
        if (existing.isEmpty() && countByChannelId(channelPin.channelId()) >= maxPins) {
            return false;
        }
        existing.ifPresent(pin -> delete(channelPin.channelId(), pin.messageId()));
        save(channelPin);
        return true;
    }

    void delete(long channelId, long messageId);

    List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    long countByChannelId(long channelId);
}
