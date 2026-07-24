package team.carrypigeon.backend.chat.domain.features.server.domain.command;

import java.util.Collection;

/**
 * 实时事件发布命令。
 *
 * @param channelId 事件所属频道 ID；不属于单一频道时为空
 * @param eventType v1 realtime 事件类型
 * @param payload 稳定事件载荷
 * @param recipientAccountIds 候选接收账号集合
 * @param applyNotificationPreferences 是否应用频道通知偏好过滤
 */
public record PublishRealtimeEventCommand(
        Long channelId,
        String eventType,
        Object payload,
        Collection<Long> recipientAccountIds,
        boolean applyNotificationPreferences
) {
}
