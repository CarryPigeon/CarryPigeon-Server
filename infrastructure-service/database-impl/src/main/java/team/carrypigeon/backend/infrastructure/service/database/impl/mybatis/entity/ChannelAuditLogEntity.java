package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道审计日志持久化实体。
 * 职责：承接 chat_channel_audit_log 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_audit_log")
@Data
public class ChannelAuditLogEntity {

    @TableId(value = "audit_id", type = IdType.INPUT)
    private Long auditId;
    private Long channelId;
    private Long actorAccountId;
    private String actionType;
    private Long targetAccountId;
    private String metadata;
    private Instant createdAt;
}
