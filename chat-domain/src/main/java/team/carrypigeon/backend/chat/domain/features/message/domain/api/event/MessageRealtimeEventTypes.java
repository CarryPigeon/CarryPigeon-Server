package team.carrypigeon.backend.chat.domain.features.message.domain.api.event;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEventType;

/**
 * 消息 feature 发布的实时事件类型。
 * 职责：集中声明消息维度的实时事件名，供发射方引用。
 * 边界：只有类型标识，不携带投递策略；新增类型只需在本类追加常量。
 */
public final class MessageRealtimeEventTypes {

    /** 频道内新消息。 */
    public static final RealtimeEventType MESSAGE_CREATED = new RealtimeEventType("message.created");

    /** 消息被撤回。 */
    public static final RealtimeEventType MESSAGE_RECALLED = new RealtimeEventType("message.recalled");

    /** 新增一条提及。 */
    public static final RealtimeEventType MENTION_CREATED = new RealtimeEventType("mention.created");

    /** 自己的已读位置更新。 */
    public static final RealtimeEventType READ_STATE_UPDATED = new RealtimeEventType("read_state.updated");

    private MessageRealtimeEventTypes() {
    }
}
