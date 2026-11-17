package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

/**
 * 实时事件发布入口
 * 职责：为业务 feature 提供唯一的下行事件发布通道，由实时通道实现承接投递
 */
public interface RealtimeEventApi {

    /** 按频道发布，仅投递到本实例已认证且仍有成员权限的在线连接；不提供跨实例路由。 */
    void publishToChannel(long channelId, RealtimeEventType type, Object payload);

    /**
     * 发布一次实时事件。
     *
     * @param event 待发布事件
     */
    void publish(RealtimeEvent event);
}
