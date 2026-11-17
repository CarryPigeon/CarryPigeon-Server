package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;

/**
 * 实时事件发布入口
 * 职责：为业务 feature 提供唯一的下行事件发布通道，由实时通道实现承接投递
 */
public interface RealtimeEventApi {

    /** 派发已持久化事件，保留事件 ID 与时间；同步编码或路由失败抛异常，调用方保留 outbox 重试。 */
    void publishToChannel(long channelId, RealtimeFrame.Event event);

    /** 按频道发布，仅投递到本实例已认证且仍有成员权限的在线连接；不提供跨实例路由。 */
    void publishToChannel(long channelId, RealtimeEventType type, Object payload);

    /**
     * 发布一次实时事件。
     *
     * @param event 待发布事件
     */
    void publish(RealtimeEvent event);
}
