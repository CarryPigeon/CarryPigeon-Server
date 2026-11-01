package team.carrypigeon.backend.infrastructure.basic.logging;

import java.util.Map;
import org.slf4j.MDC;

/**
 * MDC 日志上下文辅助工具。
 * 职责：统一管理日志上下文字段的写入与清理，避免业务代码直接散落操作 MDC。
 * 边界：作为线程上下文静态辅助能力直接暴露，不承载业务日志策略。
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
     * 写入当前日志的统一关联标识。
     * 语义：HTTP 请求中为 request id；WebSocket 中为当前帧标识。
     * 用途：统一日志输出后，可用该字段关联一次 HTTP 请求或一次 WS 帧的日志。
     *
     * @param sessionId 请求 id 或 WS 帧 id
     */
    public static void sessionId(String sessionId) {
        put(LogKeys.SESSION_ID, sessionId);
    }

    /**
     * 写入当前日志作用域。
     * 语义：HTTP 入口写为 {@link LogScope#HTTP}，WebSocket 帧入口写为 {@link LogScope#WS}。
     *
     * @param scope 日志作用域；为空时移除该字段
     */
    public static void scope(LogScope scope) {
        if (scope == null) {
            remove(LogKeys.SCOPE);
            return;
        }
        put(LogKeys.SCOPE, scope.wireName());
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
     * 约束：值会先转换为有长度上限的单行文本；空值会移除旧字段，避免线程复用时残留上下文。
     */
    public static void put(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        String sanitizedValue = LogValueSanitizer.singleLine(value);
        if (sanitizedValue.isEmpty()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, sanitizedValue);
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

    /**
     * 读取当前日志作用域。
     *
     * @return 作用域名；未设置时为 null
     */
    public static String currentScope() {
        return MDC.get(LogKeys.SCOPE);
    }

    /**
     * 读取当前端到端追踪标识。
     *
     * @return trace id；未设置时为 null
     */
    public static String currentTraceId() {
        return MDC.get(LogKeys.TRACE_ID);
    }

    /**
     * 读取当前统一关联标识。
     *
     * @return HTTP 请求 id 或 WS 帧 id；未设置时为 null
     */
    public static String currentSessionId() {
        return MDC.get(LogKeys.SESSION_ID);
    }

    /**
     * 打开隔离的 MDC 作用域。
     * 语义：进入时保存并清空当前线程上下文，关闭时清理内部字段并恢复外层上下文。
     * 约束：作用域必须由创建线程关闭，推荐使用 try-with-resources。
     *
     * @return 当前线程的日志上下文作用域
     */
    public static Scope openScope() {
        return openScope(null);
    }

    /**
     * 打开带作用域的隔离 MDC 作用域。
     * 语义：在隔离作用域内写入 {@code scope} 字段，并在关闭时恢复到外层上下文。
     *
     * @param scope 日志作用域；为空时仅隔离上下文
     * @return 当前线程的日志上下文作用域
     */
    public static Scope openScope(LogScope scope) {
        Scope result = new Scope();
        scope(scope);
        return result;
    }

    /**
     * 包装任务，使其在创建时的日志上下文中执行。
     * 用途：Netty 定时任务、线程池任务等跨线程场景，避免 trace/session/uid 丢失。
     *
     * @param task 待执行任务
     * @return 带日志上下文快照的任务
     */
    public static Runnable wrap(Runnable task) {
        return wrap(null, task);
    }

    /**
     * 包装任务，使其在创建时的日志上下文中执行，并保证作用域存在。
     *
     * @param scope 任务所属日志作用域；为空时继承快照中的作用域
     * @param task 待执行任务
     * @return 带日志上下文快照的任务
     */
    public static Runnable wrap(LogScope scope, Runnable task) {
        Map<String, String> snapshot = MDC.getCopyOfContextMap();
        return () -> {
            try (Scope ignored = openScope(scope)) {
                if (snapshot != null && !snapshot.isEmpty()) {
                    MDC.setContextMap(snapshot);
                }
                task.run();
            }
        };
    }

    /**
     * 可关闭的 MDC 隔离作用域。
     * 职责：保证线程复用和嵌套日志边界不会互相泄漏或误删上下文字段。
     */
    public static final class Scope implements AutoCloseable {

        private final Thread owner = Thread.currentThread();
        private final Map<String, String> previousContext;
        private boolean closed;

        private Scope() {
            Map<String, String> currentContext = MDC.getCopyOfContextMap();
            this.previousContext = currentContext == null ? Map.of() : Map.copyOf(currentContext);
            MDC.clear();
        }

        @Override
        public void close() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Log context scope must be closed by its owner thread");
            }
            if (closed) {
                return;
            }
            MDC.clear();
            if (!previousContext.isEmpty()) {
                MDC.setContextMap(previousContext);
            }
            closed = true;
        }
    }
}
