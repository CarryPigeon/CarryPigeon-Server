package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;

/**
 * 频道封禁持久化实体。
 * 职责：承接 chat_channel_ban 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_channel_ban")
public class ChannelBanEntity {

    @TableId(value = "ban_id", type = IdType.INPUT)
    private Long banId;
    private Long channelId;
    private Long bannedAccountId;
    private Long operatorAccountId;
    private String reason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public ChannelBanRecord toRecord() {
        return new ChannelBanRecord(
                banId == null ? 0L : banId,
                channelId == null ? 0L : channelId,
                bannedAccountId == null ? 0L : bannedAccountId,
                operatorAccountId == null ? 0L : operatorAccountId,
                reason,
                expiresAt,
                createdAt,
                revokedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 频道封禁数据库记录契约
     * @return 对应持久化实体
     */
    public static ChannelBanEntity from(ChannelBanRecord record) {
        return new ChannelBanEntity(
                record.banId(),
                record.channelId(),
                record.bannedAccountId(),
                record.operatorAccountId(),
                record.reason(),
                record.expiresAt(),
                record.createdAt(),
                record.revokedAt()
        );
    }
}
