package team.carrypigeon.backend.infrastructure.basic.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务审计日志统一入口。
 * 职责：集中维护审计 logger、事件字段格式和不可信值清洗，业务代码只表达动作与结果。
 * 边界：只负责输出审计事件，不承载业务判断、去重、落库或告警策略。
 */
public final class AuditLogs {

    /** 审计日志 logger 名称，与 log4j2 配置中的独立 appender 对应。 */
    public static final String LOGGER_NAME = "team.carrypigeon.audit";

    private static final Logger AUDIT = LoggerFactory.getLogger(LOGGER_NAME);

    private AuditLogs() {
    }

    /**
     * 记录一次成功的业务状态变更。
     *
     * @param action 稳定动作名，例如 {@code auth.login}
     * @param uid 操作者用户 id，匿名场景可为空
     * @param fields 可选字段，按 {@code key, value, key, value} 传入
     */
    public static void success(String action, Long uid, Object... fields) {
        write(action, "success", uid, null, fields);
    }

    /**
     * 记录一次业务失败。
     *
     * @param action 稳定动作名，例如 {@code auth.login}
     * @param uid 操作者用户 id，无法识别时可为空
     * @param reason 稳定失败原因，例如 {@code invalid_credentials}
     * @param fields 可选字段，按 {@code key, value, key, value} 传入
     */
    public static void failure(String action, Long uid, String reason, Object... fields) {
        write(action, "failure", uid, reason, fields);
    }

    private static void write(String action, String outcome, Long uid, String reason, Object... fields) {
        if (!AUDIT.isInfoEnabled()) {
            return;
        }
        AUDIT.info("{}", buildMessage(action, outcome, uid, reason, fields));
    }

    /**
     * 构造审计事件文本。
     * 语义：动作/原因/字段值都会收敛为单行文本；含空格、引号或等号的值会加引号，避免破坏 {@code key=value} 结构。
     *
     * @param action 动作名
     * @param outcome 结果
     * @param uid 操作者用户 id
     * @param reason 失败原因，成功时为空
     * @param fields 可选字段
     * @return 稳定审计文本
     */
    static String buildMessage(String action, String outcome, Long uid, String reason, Object... fields) {
        StringBuilder message = new StringBuilder(96);
        message.append("action=").append(safeToken(action, "unknown"));
        message.append(" outcome=").append(safeToken(outcome, "unknown"));
        message.append(" uid=").append(uid == null ? "" : uid);
        if (reason != null && !reason.isBlank()) {
            message.append(" reason=").append(formatValue(reason));
        }
        appendFields(message, fields);
        return message.toString();
    }

    private static void appendFields(StringBuilder message, Object... fields) {
        if (fields == null || fields.length < 2) {
            return;
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            String key = LogValueSanitizer.singleLine(String.valueOf(fields[index]));
            if (key.isBlank()) {
                continue;
            }
            Object value = fields[index + 1];
            if (value == null) {
                continue;
            }
            String text = LogValueSanitizer.singleLine(String.valueOf(value));
            if (text.isBlank()) {
                continue;
            }
            message.append(' ').append(key).append('=').append(formatValue(text));
        }
    }

    private static String safeToken(String value, String fallback) {
        String sanitized = LogValueSanitizer.singleLine(value);
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private static String formatValue(String value) {
        String sanitized = LogValueSanitizer.singleLine(value);
        if (sanitized.indexOf(' ') >= 0 || sanitized.indexOf('"') >= 0 || sanitized.indexOf('=') >= 0) {
            return '"' + sanitized.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return sanitized;
    }
}
