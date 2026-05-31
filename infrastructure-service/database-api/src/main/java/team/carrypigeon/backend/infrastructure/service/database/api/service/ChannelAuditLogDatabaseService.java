package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;

/**
 * 频道审计日志数据库服务抽象。
 * 职责：向 chat-domain 提供频道审计记录的最小追加式写入能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelAuditLogDatabaseService {

    /**
     * 追加写入频道审计日志。
     *
     * @param record 待持久化审计记录
     */
    void insert(ChannelAuditLogWriteRecord record);

    default List<ChannelAuditLogReadRecord> list(
            Long cursorAuditId,
            int limit,
            Long channelId,
            Long actorAccountId,
            String actionType,
            Instant fromTime,
            Instant toTime
    ) {
        throw new UnsupportedOperationException("channel audit log list is not supported");
    }
}
