package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandContext;

/**
 * 内建命令上下文。
 * 职责：在通用命令契约之外，为本 feature 内建处理器提供认证与续传能力。
 * 边界：只在前端内建处理器中使用，插件处理器仅依赖 {@link RealtimeCommandContext}；本接口不暴露任何传输实现类型。
 */
public interface RealtimeCommandScope extends RealtimeCommandContext {

    /**
     * 当前连接是否已完成认证。
     *
     * @return 已认证时返回 true
     */
    boolean isAuthenticated();

    /**
     * 校验凭证并绑定当前连接。
     * 失败语义：凭证缺失或无效时抛出领域问题异常，由分派层转换为命令错误帧。
     *
     * @param accessToken 客户端 access token
     * @param requestId 客户端请求 ID，仅用于日志
     * @param reauth 是否为凭据替换
     * @return 认证会话
     */
    RealtimeAuthSession authenticate(String accessToken, String requestId, boolean reauth);

    /**
     * 按最后事件 ID 回放离线期间的事件。
     *
     * @param accountId 账号 ID
     * @param lastEventId 最后事件 ID
     */
    void replay(long accountId, String lastEventId);
}
