package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;

/**
 * 频道已读状态持久化实体。
 * 职责：承接 chat_channel_read_state 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_channel_read_state")
public class ChannelReadStateEntity {

    @TableId(value = "channel_id", type = IdType.INPUT)
    private Long channelId;
    private Long accountId;
    private Long lastReadMessageId;
    private Instant lastReadTime;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public ChannelReadStateRecord toRecord() {
        return new ChannelReadStateRecord(
                channelId == null ? 0L : channelId,
                accountId == null ? 0L : accountId,
                lastReadMessageId == null ? 0L : lastReadMessageId,
                lastReadTime,
                createdAt,
                updatedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 频道已读状态数据库记录契约
     * @return 对应持久化实体
     */
    public static ChannelReadStateEntity from(ChannelReadStateRecord record) {
        return new ChannelReadStateEntity(
                record.channelId(),
                record.accountId(),
                record.lastReadMessageId(),
                record.lastReadTime(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
