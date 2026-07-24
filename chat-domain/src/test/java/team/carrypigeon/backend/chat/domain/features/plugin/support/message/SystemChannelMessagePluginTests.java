package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * SystemChannelMessagePlugin 契约测试。
 * 职责：验证 system 消息插件的最小摘要与 payload 生成语义。
 * 边界：只验证 system 插件自身规则，不覆盖内部发送编排。
 */
@Tag("contract")
class SystemChannelMessagePluginTests {

    /**
     * 验证 system 消息插件会生成稳定 system 预览文本。
     */
    @Test
    @DisplayName("validate canonical data valid system data builds preview text")
    void validateCanonicalData_validSystemData_buildsPreviewText() {
        SystemChannelMessagePlugin plugin = new SystemChannelMessagePlugin();

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of("text", "maintenance notice", "severity", "info")
        );

        assertEquals("maintenance notice", canonical.data().get("text"));
        assertEquals("info", canonical.data().get("severity"));
        assertEquals("[系统消息] maintenance notice", canonical.preview());
    }

    /**
     * 验证系统消息 domain 不能通过面向客户端的 canonical HTTP 入口发送。
     */
    @Test
    @DisplayName("system domain is not client sendable")
    void clientSendable_systemDomain_returnsFalse() {
        SystemChannelMessagePlugin plugin = new SystemChannelMessagePlugin();

        assertFalse(plugin.clientSendable());
    }
}
