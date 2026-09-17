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

    /**
     * 在数据库事务内替换指定消息的置顶记录，并原子执行频道置顶数量上限校验。
     *
     * @return 成功写入或替换时返回 {@code true}；达到上限且目标消息尚未置顶时返回 {@code false}
     */
    boolean replaceWithinLimit(ChannelPinRecord record, long maxPins);

    void delete(long channelId, long messageId);

    List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    long countByChannelId(long channelId);
}
