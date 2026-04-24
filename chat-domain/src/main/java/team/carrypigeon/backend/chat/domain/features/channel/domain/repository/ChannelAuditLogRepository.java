package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;

/**
 * 频道审计日志仓储抽象。
 * 职责：定义频道治理审计记录的追加式写入入口。
 * 边界：当前只提供最小落库能力，不暴露存储实现细节。
 */
public interface ChannelAuditLogRepository {

    /**
     * 追加一条频道审计日志。
     *
     * @param channelAuditLog 审计记录
     */
    void append(ChannelAuditLog channelAuditLog);
}
