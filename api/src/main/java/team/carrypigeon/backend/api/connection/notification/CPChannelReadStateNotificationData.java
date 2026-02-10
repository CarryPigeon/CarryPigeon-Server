package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 频道已读状态变更通知数据。
 * <p>
 * 用于同一用户多终端同步已读进度。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPChannelReadStateNotificationData {

    /**
     * 频道 ID。
     */
    private long cid;

    /**
     * 用户 ID。
     */
    private long uid;

    /**
     * 最近一次已读时间（毫秒时间戳）。
     */
    private long lastReadTime;
}
