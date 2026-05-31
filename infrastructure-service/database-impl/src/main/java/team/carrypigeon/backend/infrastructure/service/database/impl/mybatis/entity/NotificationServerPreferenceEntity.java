package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 服务端级通知偏好持久化实体。
 */
@TableName("chat_notification_server_preference")
@Data
public class NotificationServerPreferenceEntity {

    @TableId("account_id")
    private Long accountId;
    private String mode;
    private Long mutedUntil;
    private Instant createdAt;
    private Instant updatedAt;
}
