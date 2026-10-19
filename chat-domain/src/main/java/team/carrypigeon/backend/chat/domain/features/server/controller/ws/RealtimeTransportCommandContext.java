package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandContext;

/**
 * 传输层命令上下文。
 * 职责：在通用命令契约之外，为本 feature 内建处理器提供认证与续传机制。
 * 边界：仅在本 feature 传输层内部可见，插件处理器只能使用 {@link RealtimeCommandContext}。
 */
interface RealtimeTransportCommandContext extends RealtimeCommandContext {

    /**
     * 当前 Netty 通道上下文。
     *
     * @return Netty 通道上下文
     */
    ChannelHandlerContext nettyContext();

    /**
     * 认证机制。
     *
     * @return 认证协调器
     */
    RealtimeAuthenticationCoordinator authenticationCoordinator();

    /**
     * 按最后事件 ID 回放离线期间的事件。
     *
     * @param accountId 账号 ID
     * @param lastEventId 最后事件 ID
     */
    void replay(long accountId, String lastEventId);
}
