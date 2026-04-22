package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

/**
 * 频道数据库服务抽象。
 * 职责：向 chat-domain 提供频道最小查询能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelDatabaseService {

    /**
     * 查询默认频道。
     *
     * @return 命中时返回默认频道记录
     */
    Optional<ChannelRecord> findDefaultChannel();

    /**
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道记录
     */
    Optional<ChannelRecord> findById(long channelId);
}
