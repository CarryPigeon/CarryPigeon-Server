package team.carrypigeon.backend.infrastructure.basic.logging;

/**
 * 日志上下文字段名规范。
 * 职责：统一 MDC key，避免不同模块各自定义日志字段名。
 * 边界：这里只定义字段名，不决定具体业务何时写入字段值。
 */
public final class LogKeys {

    /** 日志来源通道，取值见 {@link LogScope}。 */
    public static final String SCOPE = "scope";

    /** 端到端追踪标识；WebSocket 帧中与 session id 相同。 */
    public static final String TRACE_ID = "trace_id";

    /** 统一关联标识：HTTP 请求 id 或 WebSocket 单帧 id。 */
    public static final String SESSION_ID = "session_id";

    /** 路由或通道标识。 */
    public static final String ROUTE = "route";

    /** 已认证用户 id。 */
    public static final String UID = "uid";

    private LogKeys() {}
}
