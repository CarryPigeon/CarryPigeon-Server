package team.carrypigeon.backend.chat.domain.features.server.domain.capability;

import java.util.Collection;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEvent;

/**
 * 实时事件投递能力。
 * 职责：把一次已确定接收人的实时事件交给具体传输实现。
 * 边界：只负责投递；不判断接收人、不做业务决策、不感知调用方事务。
 */
public interface RealtimeEventSink {

    /**
     * 投递一次实时事件。
     * 输入：事件快照与最终接收账号。
     * 约束：接收人必须非空；实现必须保证事件先进入可续传窗口再广播。
     *
     * @param event 事件快照
     * @param recipients 最终接收账号
     */
    void deliver(RealtimeEvent event, Collection<Long> recipients);
}
