package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 频道成员变更通知数据。
 * <p>
 * 常用于 `/core/channel/member/list` 路由的推送负载。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPChannelMemberNotificationData {

    /**
     * 变更类型，例如：join / leave / admin_add / admin_remove。
     */
    private String type;

    /**
     * 频道 ID。
     */
    private long cid;

    /**
     * 受影响的用户 ID。
     */
    private long uid;
}
