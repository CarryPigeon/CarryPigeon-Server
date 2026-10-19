package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

import java.util.List;
import java.util.Objects;

/**
 * 一次待下发的实时事件。
 * 职责：承载事件类型、载荷与候选接收账号，供实时通道编码并投递。
 * 边界：不携带投递策略，不含传输或 JSON 细节；载荷由发射方定义，实时通道不解释其内容。
 *
 * @param type 事件类型
 * @param payload 由发射方定义的不可变载荷
 * @param recipientAccountIds 候选接收账号，构造时去空去重并做不可变拷贝
 */
public record RealtimeEvent(
        RealtimeEventType type,
        Object payload,
        List<Long> recipientAccountIds
) {

    public RealtimeEvent {
        Objects.requireNonNull(type, "type must not be null");
        recipientAccountIds = recipientAccountIds == null
                ? List.of()
                : recipientAccountIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }
}
