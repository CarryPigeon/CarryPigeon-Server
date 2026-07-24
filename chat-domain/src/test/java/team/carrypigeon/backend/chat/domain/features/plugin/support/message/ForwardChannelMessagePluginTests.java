package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ForwardChannelMessagePlugin 契约测试。
 * 职责：验证转发来源、附言与合并来源的 canonical data 校验。
 * 边界：不读取源消息或判断频道权限，只验证插件 schema。
 */
@Tag("contract")
class ForwardChannelMessagePluginTests {

    private static final ChannelMessageBuildContext CONTEXT = new ChannelMessageBuildContext(
            5001L,
            1L,
            1001L,
            Instant.parse("2026-04-22T00:00:00Z")
    );

    /**
     * 验证单条转发的来源和附言被规范化并保留在 data。
     */
    @Test
    @DisplayName("validate canonical data valid single forward preserves source")
    void validateCanonicalData_validSingleForward_preservesSource() {
        ForwardChannelMessagePlugin plugin = new ForwardChannelMessagePlugin();

        var canonical = plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                "domain", "Core:Text",
                "domain_version", "1.0.0",
                "content", Map.of("text", " check "),
                "forwarded_from", source("5000")
        ));

        assertEquals("5000", ((Map<?, ?>) canonical.data().get("forwarded_from")).get("mid"));
        assertEquals("check", ((Map<?, ?>) canonical.data().get("content")).get("text"));
        assertEquals("check", canonical.preview());
        assertFalse(plugin.clientSendable());
    }

    /**
     * 验证合并转发至少需要两条来源。
     */
    @Test
    @DisplayName("validate canonical data single merged source throws validation problem")
    void validateCanonicalData_singleMergedSource_throwsValidationProblem() {
        ForwardChannelMessagePlugin plugin = new ForwardChannelMessagePlugin();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                        "domain", "Core:Text",
                        "domain_version", "1.0.0",
                        "forwarded_messages", List.of(source("5000"))
                ))
        );

        assertEquals("forwarded_messages must contain at least two sources", exception.getMessage());
    }

    /**
     * 验证来源快照的 ID 类型异常会被插件拒绝。
     */
    @Test
    @DisplayName("validate canonical data numeric source id throws validation problem")
    void validateCanonicalData_numericSourceId_throwsValidationProblem() {
        ForwardChannelMessagePlugin plugin = new ForwardChannelMessagePlugin();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                        "domain", "Core:Text",
                        "domain_version", "1.0.0",
                        "forwarded_from", Map.of(
                                "mid", 5000L,
                                "cid", "1",
                                "uid", "1001",
                                "preview", "source",
                                "send_time", 1L
                        )
                ))
        );

        assertEquals("forward source mid must be decimal snowflake string", exception.getMessage());
    }

    private static Map<String, Object> source(String messageId) {
        return Map.of(
                "mid", messageId,
                "cid", "1",
                "uid", "1001",
                "preview", "source",
                "send_time", 1L
        );
    }
}
