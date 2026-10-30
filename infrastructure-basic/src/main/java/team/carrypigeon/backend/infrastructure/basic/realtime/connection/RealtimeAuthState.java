package team.carrypigeon.backend.infrastructure.basic.realtime.connection;

/**
 * 实时通信接口认证状态
 */
public interface RealtimeAuthState {

    /**
     * 认证会话
     */
    RealtimeAuthSession authenticate(
        RealtimeConnection connection,
        RealtimeAttributes attributes,
        RealtimeScheduler scheduler,   // 🆕
        String accessToken
    );

    /**
     * 认证会话失效
     */
    void onChannelInactive(
        RealtimeConnection connection,
        RealtimeAttributes attributes,
        RealtimeScheduler scheduler    // 🆕
    );
}
