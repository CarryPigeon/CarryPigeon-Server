package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import java.time.Instant;

import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

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

    /**
     * 将查询投影转换为数据库记录契约。
     * 约束：聚合列为空时按 0 与 null 输出，不伪造已读时间。
     *
     * @return 对应数据库记录契约
     */
    public ChannelUnreadRecord toRecord() {
        return new ChannelUnreadRecord(
                channelId == null ? 0L : channelId,
                unreadCount == null ? 0L : unreadCount,
                lastReadTime
        );
    }
}
