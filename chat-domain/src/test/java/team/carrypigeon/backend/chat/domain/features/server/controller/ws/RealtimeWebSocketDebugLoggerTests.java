package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeWebSocketDebugLogger 契约测试。
 * 职责：验证本地 WS 调试日志构造辅助能力不会泄露常见敏感查询参数。
 * 边界：不依赖具体日志 appender，只验证日志摘要前置脱敏规则。
 */
@Tag("contract")
class RealtimeWebSocketDebugLoggerTests {

    /**
     * 验证 `sanitizeUri` 在包含敏感查询参数时会遮蔽参数值。
     */
    @Test
    @DisplayName("sanitize uri masks sensitive query parameters")
    void sanitizeUri_sensitiveQuery_masksValues() {
        RealtimeWebSocketDebugLogger logger = new RealtimeWebSocketDebugLogger(true);

        String sanitized = logger.sanitizeUri("/api/ws?access_token=abc&plain=1&secret=hidden");

        assertThat(sanitized)
                .isEqualTo("/api/ws?access_token=***&plain=1&secret=***")
                .doesNotContain("abc")
                .doesNotContain("hidden");
    }
}
