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
 * ReplyTextChannelMessagePlugin 契约测试。
 * 职责：验证回复消息嵌套 data、关系锚点和 Wire ID 的严格校验。
 * 边界：只验证 ReplyText 插件自身规则，不覆盖权限和消息持久化。
 */
@Tag("contract")
class ReplyTextChannelMessagePluginTests {

    private static final ChannelMessageBuildContext CONTEXT = new ChannelMessageBuildContext(
            5001L,
            1L,
            1001L,
            Instant.parse("2026-04-22T00:00:00Z")
    );

    /**
     * 验证合法回复快照会被保留为 canonical data。
     */
    @Test
    @DisplayName("validate canonical data valid reply preserves nested relation")
    void validateCanonicalData_validReply_preservesNestedRelation() {
        ReplyTextChannelMessagePlugin plugin = new ReplyTextChannelMessagePlugin();

        var canonical = plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                "content", Map.of("text", " reply "),
                "reply_to_mid", "4999",
                "reply_to", Map.of(
                        "mid", "4999",
                        "sender_name", "Bob",
                        "preview", "source",
                        "created_at", 1L,
                        "unavailable", false
                )
        ));

        assertEquals("reply", ((Map<?, ?>) canonical.data().get("content")).get("text"));
        assertEquals("4999", canonical.data().get("reply_to_mid"));
        assertEquals("reply", canonical.preview());
    }

    /**
     * 验证回复快照必须是 JSON 对象，不能以字符串污染 data。
     */
    @Test
    @DisplayName("validate canonical data string reply snapshot throws validation problem")
    void validateCanonicalData_stringReplySnapshot_throwsValidationProblem() {
        ReplyTextChannelMessagePlugin plugin = new ReplyTextChannelMessagePlugin();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                        "content", Map.of("text", "reply"),
                        "reply_to_mid", "4999",
                        "reply_to", "invalid"
                ))
        );

        assertEquals("reply_to must be object", exception.getMessage());
    }

    /**
     * 验证回复锚点和快照 ID 不一致时拒绝 canonical data。
     */
    @Test
    @DisplayName("validate canonical data mismatched reply id throws validation problem")
    void validateCanonicalData_mismatchedReplyId_throwsValidationProblem() {
        ReplyTextChannelMessagePlugin plugin = new ReplyTextChannelMessagePlugin();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                plugin.validateCanonicalData(CONTEXT, "1.0.0", Map.of(
                        "content", Map.of("text", "reply"),
                        "reply_to_mid", "4999",
                        "reply_to", Map.of(
                                "mid", "4998",
                                "sender_name", "Bob",
                                "preview", "source",
                                "created_at", 1L
                        )
                ))
        );

        assertEquals("reply_to_mid must match reply_to.mid", exception.getMessage());
    }
}
