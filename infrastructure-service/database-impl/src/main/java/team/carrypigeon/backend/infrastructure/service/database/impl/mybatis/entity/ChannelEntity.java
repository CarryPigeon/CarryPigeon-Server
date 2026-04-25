package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道持久化实体。
 * 职责：承接 chat_channel 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel")
@Data
public class ChannelEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long conversationId;
    private String name;
    private String type;
    @TableField("is_default")
    private Boolean defaultChannel;
    private Instant createdAt;
    private Instant updatedAt;
}
