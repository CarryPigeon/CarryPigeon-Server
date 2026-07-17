package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import java.time.Instant;
import lombok.Data;

/**
 * 频道未读查询投影。
 * 职责：承接未读聚合 SQL 返回的频道、未读数和已读时间字段。
 * 边界：仅供 database-impl 内部未读读链路使用。
 */
@Data
public class ChannelUnreadProjection {
    private Long channelId;
    private Long unreadCount;
    private Instant lastReadTime;
}
