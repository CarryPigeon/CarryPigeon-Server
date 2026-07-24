package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PluginChannelMessagePlugin 契约测试。
 * 职责：验证插件消息插件的结构化载荷与插件标识校验语义。
 * 边界：只验证 plugin 消息插件自身规则，不覆盖领域服务编排。
 */
@Tag("contract")
class PluginChannelMessagePluginTests {

    /**
     * 验证插件消息插件会生成稳定摘要和 canonical payload。
     */
    @Test
    @DisplayName("validate canonical data valid plugin data builds canonical payload")
    void validateCanonicalData_validPluginData_buildsCanonicalPayload() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin("test-extension");

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of(
                        "plugin_key", "test-extension",
                        "payload", Map.of("event", "player_join"),
                        "text", "extension bridge"
                )
        );

        assertEquals("extension bridge", canonical.data().get("text"));
        assertEquals("[插件消息] extension bridge", canonical.preview());
    }

    /**
     * 验证缺少 pluginKey 时返回稳定校验问题。
     */
    @Test
    @DisplayName("validate canonical data blank plugin key throws validation problem")
    void validateCanonicalData_blankPluginKey_throwsValidationProblem() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin("test-extension");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of("plugin_key", " ", "payload", Map.of("event", "player_join"))
                )
        );

        assertEquals("plugin_key must not be blank", exception.getMessage());
    }

    /**
     * 验证扩展插件会保留经自身 schema 校验的结构化 payload。
     */
    @Test
    @DisplayName("validate canonical data valid payload preserves extension fields")
    void validateCanonicalData_validPayload_preservesExtensionFields() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin("test-extension");
        Map<String, Object> payload = Map.of("event", "player_join", "level", 3);

        var canonical = plugin.validateCanonicalData(
                new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                "1.0.0",
                Map.of("plugin_key", "test-extension", "payload", payload, "text", "joined")
        );

        assertEquals(payload, canonical.data().get("payload"));
        assertEquals("[插件消息] joined", canonical.preview());
    }

    /**
     * 验证扩展插件拒绝非对象 payload。
     */
    @Test
    @DisplayName("validate canonical data malformed payload throws validation problem")
    void validateCanonicalData_malformedPayload_throwsValidationProblem() {
        PluginChannelMessagePlugin plugin = new PluginChannelMessagePlugin("test-extension");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.validateCanonicalData(
                        new ChannelMessageBuildContext(5001L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        "1.0.0",
                        Map.of("plugin_key", "test-extension", "payload", "invalid")
                )
        );

        assertEquals("payload must be object", exception.getMessage());
    }
}
