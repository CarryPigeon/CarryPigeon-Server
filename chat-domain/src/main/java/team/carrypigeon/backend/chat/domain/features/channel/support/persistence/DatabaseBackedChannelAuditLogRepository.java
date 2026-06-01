package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelAuditLogDatabaseService;

/**
 * 基于 database-api 的频道审计日志仓储适配器。
 * 职责：在 channel feature 内完成审计领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedChannelAuditLogRepository implements ChannelAuditLogRepository {

    private final ChannelAuditLogDatabaseService channelAuditLogDatabaseService;

    public DatabaseBackedChannelAuditLogRepository(ChannelAuditLogDatabaseService channelAuditLogDatabaseService) {
        this.channelAuditLogDatabaseService = channelAuditLogDatabaseService;
    }

    /**
     * 追加一条频道审计日志。
     * 输入：领域层构造完成的审计事件。
     * 副作用：向数据库追加一条只增不改的审计记录。
     *
     * @param channelAuditLog 频道审计事件
     */
    @Override
    public void append(ChannelAuditLog channelAuditLog) {
        channelAuditLogDatabaseService.insert(new ChannelAuditLogWriteRecord(
                channelAuditLog.auditId(),
                channelAuditLog.channelId(),
                channelAuditLog.actorAccountId(),
                channelAuditLog.actionType(),
                channelAuditLog.targetAccountId(),
                channelAuditLog.metadata(),
                channelAuditLog.createdAt()
        ));
    }

    @Override
    public List<ChannelAuditLog> list(
            Long cursorAuditId,
            int limit,
            Long channelId,
            Long actorAccountId,
            String actionType,
            Instant fromTime,
            Instant toTime
    ) {
        return channelAuditLogDatabaseService.list(cursorAuditId, limit, channelId, actorAccountId, actionType, fromTime, toTime)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ChannelAuditLog toDomain(ChannelAuditLogReadRecord record) {
        return new ChannelAuditLog(record.auditId(), record.channelId(), record.actorAccountId(), record.actionType(), null, record.metadata(), record.createdAt());
    }
}
