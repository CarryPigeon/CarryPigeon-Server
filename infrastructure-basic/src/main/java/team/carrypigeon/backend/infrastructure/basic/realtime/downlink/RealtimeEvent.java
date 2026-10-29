package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

import java.util.List;

/**
 * 一次待下发的实时事件。
 * 职责：承载事件类型、载荷与候选接收账号，供实时通道编码并投递。
 *
 * @param type 事件类型
 * @param payload 由发射方定义的不可变载荷
 * @param recipientAccountIds 候选接收账号
 */
public record RealtimeEvent(
        RealtimeEventType type,
        Object payload,
        List<Long> recipientAccountIds
) {
}
