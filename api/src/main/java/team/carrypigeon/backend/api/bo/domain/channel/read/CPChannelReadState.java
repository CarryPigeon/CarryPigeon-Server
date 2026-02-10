package team.carrypigeon.backend.api.bo.domain.channel.read;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户在频道内的已读状态。
 * <p>
 * 该对象用于持久化“某用户在某频道读到了哪里”，以支持同一账号多端间未读数与已读进度同步。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelReadState {

    /** 主键 ID。 */
    private long id;
    /** 用户 ID。 */
    private long uid;
    /** 频道 ID。 */
    private long cid;
    /** 最近一次已读消息 ID（0 表示从未读过）。 */
    private long lastReadMid;
    /** 最近一次已读时间（毫秒时间戳，0 表示从未读过）。 */
    private long lastReadTime;
}
