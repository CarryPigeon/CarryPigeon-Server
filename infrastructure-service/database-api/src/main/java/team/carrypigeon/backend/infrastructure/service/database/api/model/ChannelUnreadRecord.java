package team.carrypigeon.backend.infrastructure.service.database.api.model;

/**
 * 频道未读读侧数据库投影。
 * 职责：表达按账户聚合出的频道未读数与已读锚点时间。
 * 边界：不是表行，只服务未读统计读链路，不承载未读计算规则。
 *
 * @param channelId 频道 ID
 * @param unreadCount 该频道未读消息数，只返回大于 0 的频道
 */
public record ChannelUnreadRecord(
        long channelId,
        long unreadCount
) {
}
