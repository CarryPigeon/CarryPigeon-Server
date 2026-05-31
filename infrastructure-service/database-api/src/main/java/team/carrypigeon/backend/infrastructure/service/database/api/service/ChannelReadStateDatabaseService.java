package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

/**
 * 频道已读状态数据库服务抽象。
 */
public interface ChannelReadStateDatabaseService {

    Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId);

    void upsert(ChannelReadStateRecord record);

    List<ChannelUnreadRecord> listUnreadsByAccountId(long accountId);
}
