package team.carrypigeon.backend.infrastructure.basic.realtime.uplink;

import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;

/**
 * 实时通道上行命令处理器。
 * 职责：处理一条客户端命令，并通过上下文回复结果。
 * 边界：不管理连接生命周期、认证计时、续传流程；不接触具体传输实现类型。
 */
public interface RealtimeCommandHandler {

    /**
     * 命令类型，与客户端帧的 type 字段一致。
     * 约束：全局唯一；重复注册会导致启动失败。
     *
     * @return 命令类型
     */
    String commandType();

    /**
     * 是否要求连接已完成认证。
     *
     * @return 需要认证时返回 true
     */
    boolean requiresAuthentication();

    /**
     * 处理一条命令。
     *
     * @param message           命令数据
     * @param connection        当前连接
     * @param attributes        连接属性
     * @param scheduler         调度器
     * @param realtimeAuthState 认证状态
     */
    void handle(
        RealtimeClientMessage message,
        RealtimeConnection connection,
        RealtimeAttributes attributes,
        RealtimeScheduler scheduler,
        RealtimeAuthState realtimeAuthState
    );
}
