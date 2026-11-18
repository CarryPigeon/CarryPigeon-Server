package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.readstate;

import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

/**
 * 频道未读查询投影。
 * 职责：承接未读聚合 SQL 返回的频道与未读数。
 * 边界：仅供 database-impl 内部未读读链路使用。
 */
@Data
public class ChannelUnreadProjection {

    private Long channelId;
    private Long unreadCount;

    /**
     * 将查询投影转换为数据库记录契约。
     * 约束：未读数达到 10000 后不再精确统计超出部分。
     *
     * @return 对应数据库记录契约
     */
    public ChannelUnreadRecord toRecord() {
        return new ChannelUnreadRecord(
                channelId == null ? 0L : channelId,
                unreadCount == null ? 0L : unreadCount
        );
    }
}
