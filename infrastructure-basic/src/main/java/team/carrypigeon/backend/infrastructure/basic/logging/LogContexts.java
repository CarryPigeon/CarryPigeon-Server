package team.carrypigeon.backend.infrastructure.basic.logging;

import java.util.Map;
import org.slf4j.MDC;

/**
 * MDC 日志上下文辅助工具。
 * 职责：统一管理日志上下文字段的写入与清理，避免业务代码直接散落操作 MDC。
 * 边界：这里只处理日志上下文，不承载业务日志策略。
 */
public final class LogContexts {

    private LogContexts() {
    }

    public static void traceId(String traceId) {
        put(LogKeys.TRACE_ID, traceId);
    }

    public static void requestId(String requestId) {
        put(LogKeys.REQUEST_ID, requestId);
    }

    public static void route(String route) {
        put(LogKeys.ROUTE, route);
    }

    public static void uid(String uid) {
        put(LogKeys.UID, uid);
    }

    public static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        MDC.put(key, value);
    }

    public static void putAll(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.forEach(LogContexts::put);
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    public static void clear() {
        MDC.clear();
    }
}
