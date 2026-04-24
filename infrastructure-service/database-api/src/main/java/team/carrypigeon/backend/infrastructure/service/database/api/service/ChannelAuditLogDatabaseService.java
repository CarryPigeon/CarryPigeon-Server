package team.carrypigeon.backend.infrastructure.service.database.api.service;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogRecord;

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
    void insert(ChannelAuditLogRecord record);
}
