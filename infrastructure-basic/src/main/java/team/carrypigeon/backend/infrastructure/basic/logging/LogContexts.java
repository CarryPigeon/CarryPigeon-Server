package team.carrypigeon.backend.infrastructure.basic.logging;

import java.util.Map;
import org.slf4j.MDC;

/**
 * TODO: LogContexts直接向domain层进行了暴露而不是通过InfrastructureBasics对象，应该转变暴露方式
 * MDC 日志上下文辅助工具。
 * 职责：统一管理日志上下文字段的写入与清理，避免业务代码直接散落操作 MDC。
 * 边界：这里只处理日志上下文，不承载业务日志策略。
 */
public final class LogContexts {

    private LogContexts() {
    }

    /**
     * 写入当前线程的 trace id。
     */
    public static void traceId(String traceId) {
        put(LogKeys.TRACE_ID, traceId);
    }

    /**
     * 写入当前请求的 request id。
     */
    public static void requestId(String requestId) {
        put(LogKeys.REQUEST_ID, requestId);
    }

    /**
     * 写入当前路由标识。
     */
    public static void route(String route) {
        put(LogKeys.ROUTE, route);
    }

    /**
     * 写入当前登录用户标识。
     */
    public static void uid(String uid) {
        put(LogKeys.UID, uid);
    }

    /**
     * 向 MDC 写入单个字段。
     * 约束：空值与空白值会被直接忽略，避免污染日志上下文。
     */
    public static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        MDC.put(key, value);
    }

    /**
     * 批量写入 MDC 字段。
     */
    public static void putAll(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.forEach(LogContexts::put);
    }

    /**
     * 从 MDC 中移除指定字段。
     */
    public static void remove(String key) {
        MDC.remove(key);
    }

    /**
     * 清空当前线程持有的全部 MDC 上下文。
     */
    public static void clear() {
        MDC.clear();
    }
}
