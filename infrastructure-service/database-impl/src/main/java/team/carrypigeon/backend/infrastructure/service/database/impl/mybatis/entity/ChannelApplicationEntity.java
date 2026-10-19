package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;

/**
 * 频道申请持久化实体。
 * 职责：承接 chat_channel_application 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_channel_application")
public class ChannelApplicationEntity {

    @TableId(value = "application_id", type = IdType.INPUT)
    private Long applicationId;
    private Long channelId;
    private Long applicantId;
    private String reason;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public ChannelApplicationRecord toRecord() {
        return new ChannelApplicationRecord(
                applicationId,
                applicantId,
                channelId,
                reason,
                status,
                createdAt,
                respondedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 频道申请数据库记录契约
     * @return 对应持久化实体
     */
    public static ChannelApplicationEntity from(ChannelApplicationRecord record) {
        return new ChannelApplicationEntity(
                record.applicationId(),
                record.channelId(),
                record.applicantId(),
                record.reason(),
                record.status(),
                record.createdAt(),
                record.respondedAt()
        );
    }
}
