package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
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

    /** 判断频道是否存在任意审计日志记录。 */
    boolean existsByChannelId(long channelId);

    List<ChannelAuditLogReadRecord> list(
            Long cursorAuditId,
            int limit,
            Long channelId,
            Long actorAccountId,
            String actionType,
            Instant fromTime,
            Instant toTime
    );

    /** 按频道集合批量查询审计日志。 */
    List<ChannelAuditLogReadRecord> listByChannelIds(
            Long cursorAuditId,
            int limit,
            Collection<Long> channelIds,
            Long actorAccountId,
            String actionType,
            Instant fromTime,
            Instant toTime
    );
}
