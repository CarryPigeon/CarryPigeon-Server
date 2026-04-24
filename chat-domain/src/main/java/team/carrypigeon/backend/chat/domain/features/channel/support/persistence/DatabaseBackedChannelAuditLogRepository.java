package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogRecord;
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

    @Override
    public void append(ChannelAuditLog channelAuditLog) {
        channelAuditLogDatabaseService.insert(new ChannelAuditLogRecord(
                channelAuditLog.auditId(),
                channelAuditLog.channelId(),
                channelAuditLog.actorAccountId(),
                channelAuditLog.actionType(),
                channelAuditLog.targetAccountId(),
                channelAuditLog.metadata(),
                channelAuditLog.createdAt()
        ));
    }
}
