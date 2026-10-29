package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

/**
 * 实时事件发布入口
 * 职责：为业务 feature 提供唯一的下行事件发布通道，由实时通道实现承接投递
 */
public interface RealtimeEventApi {

    /**
     * 发布一次实时事件。
     *
     * @param event 待发布事件
     */
    void publish(RealtimeEvent event);
}
