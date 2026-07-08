package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;

/**
 * 频道置顶数据库服务抽象。
 */
public interface ChannelPinDatabaseService {

    Optional<ChannelPinRecord> findByChannelIdAndMessageId(long channelId, long messageId);

    void insert(ChannelPinRecord record);

    void delete(long channelId, long messageId);

    default void deleteByMessageId(long messageId) {
        throw new UnsupportedOperationException("channel pin delete by message is not supported");
    }

    List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    long countByChannelId(long channelId);
}
