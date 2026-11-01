package team.carrypigeon.backend.infrastructure.basic.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 审计日志文本格式测试。
 * 职责：验证事件字段顺序、空值处理和不可信值引用，避免业务日志格式漂移。
 */
class AuditLogsTests {

    @Test
    void formatsSuccessWithStableFields() {
        assertEquals(
                "action=auth.login outcome=success uid=42 method=password",
                AuditLogs.buildMessage("auth.login", "success", 42L, null, "method", "password")
        );
    }

    @Test
    void formatsFailureAndQuotesUntrustedValues() {
        assertEquals(
                "action=auth.login outcome=failure uid= reason=\"invalid credentials\"",
                AuditLogs.buildMessage("auth.login", "failure", null, "invalid credentials")
        );
    }

    @Test
    void skipsNullAndBlankFields() {
        assertEquals(
                "action=channel.create outcome=success uid=7 channel_id=100",
                AuditLogs.buildMessage(
                        "channel.create", "success", 7L, null,
                        "channel_id", 100L,
                        "empty", "   ",
                        "missing", null
                )
        );
    }

    @Test
    void fallsBackWhenActionIsBlank() {
        assertEquals(
                "action=unknown outcome=success uid=",
                AuditLogs.buildMessage("   ", "success", null, null)
        );
    }
}
