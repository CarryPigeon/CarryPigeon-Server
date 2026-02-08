package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 消息通知的 data 载荷（对应 {@code route="/core/message"}）。
 * <p>
 * 注意：该对象仅描述“通知”的内层字段；外层仍由 {@code CPResponse(id=-1, code=0)} 封装。
 * </p>
 */
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
    /**
     * 消息摘要（用于通知提示文本；例如删除消息时的固定短语）。
     */
    private String sContent;
    /**
     * 消息 ID。
     */
    private long mid;
    /**
     * 频道 ID。
     */
    private long cid;
    /**
     * 发送者用户 ID。
     */
    private long uid;
    /**
     * 消息发送时间（epoch millis）。
     */
    private long sendTime;
}
