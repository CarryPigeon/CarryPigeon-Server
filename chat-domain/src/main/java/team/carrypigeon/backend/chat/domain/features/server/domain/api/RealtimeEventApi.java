package team.carrypigeon.backend.chat.domain.features.server.domain.api;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEvent;

/**
 * 实时事件发布入口。
 * 职责：为其它 feature 提供唯一的下行事件发布通道，负责接收人规范化与失败隔离。
 * 边界：只表达"某事件已发生、需要通知这些账号"；不解释载荷，不做业务查询，不向外抛出异常。
 * 调用时机：业务事务提交之后。
 */
public interface RealtimeEventApi {

    /**
     * 发布一次实时事件。
     * 语义：尽力而为。接收人为空时不投递；投递失败只记录日志，不影响调用方已提交的事务。
     *
     * @param event 待发布事件
     */
    void publish(RealtimeEvent event);
}
