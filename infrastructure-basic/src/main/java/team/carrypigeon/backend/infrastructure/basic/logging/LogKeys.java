package team.carrypigeon.backend.infrastructure.basic.logging;

/**
 * 日志上下文字段名规范。
 * 职责：统一 MDC key，避免不同模块各自定义日志字段名。
 * 边界：这里只定义字段名，不决定具体业务何时写入字段值。
 */
public final class LogKeys {

    public static final String TRACE_ID = "trace_id";
    public static final String REQUEST_ID = "request_id";
    public static final String ROUTE = "route";
    public static final String UID = "uid";

    private LogKeys() {
    }
}
