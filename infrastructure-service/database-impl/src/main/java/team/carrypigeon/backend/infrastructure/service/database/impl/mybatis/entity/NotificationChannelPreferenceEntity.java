package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道级通知偏好持久化实体。
 */
@TableName("chat_notification_channel_preference")
@Data
public class NotificationChannelPreferenceEntity {

    private Long accountId;
    private Long channelId;
    private String mode;
    private Long mutedUntil;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Long id;
}
