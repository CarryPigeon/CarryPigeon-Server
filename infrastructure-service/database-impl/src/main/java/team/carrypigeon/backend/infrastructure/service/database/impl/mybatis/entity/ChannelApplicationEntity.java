package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;

/**
 * 频道邀请持久化实体。
 * 职责：承接 chat_channel_application 表字段与 MyBatis 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_application")
@Data
public class ChannelApplicationEntity {
    @TableId
    private Long applicationId;
    private Long channelId;
    private Long applicantId;
    private String reason;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;

    public ChannelApplicationRecord toRecord(){
        return new ChannelApplicationRecord(
            applicationId,
            channelId,
            applicantId,
            reason,
            status,
            createdAt,
            respondedAt
        );
    }

    public static ChannelApplicationEntity from(ChannelApplicationRecord record){
        ChannelApplicationEntity entity = new ChannelApplicationEntity();
        entity.applicationId = record.applicationId();
        entity.channelId = record.channelId();
        entity.applicantId = record.applicantId();
        entity.reason = record.reason();
        entity.status = record.status();
        entity.createdAt = record.createdAt();
        entity.respondedAt = record.respondedAt();
        return entity;
    }
}
