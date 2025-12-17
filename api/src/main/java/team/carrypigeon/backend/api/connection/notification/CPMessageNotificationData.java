package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPMessageNotificationData {
    /**
     * 消息推送类型，例如：
     * - "create"：新消息
     * - "delete"：删除消息
     */
    private String type;
    private String sContent;
    private long mid;
    private long cid;
    private long uid;
    private long sendTime;
}
