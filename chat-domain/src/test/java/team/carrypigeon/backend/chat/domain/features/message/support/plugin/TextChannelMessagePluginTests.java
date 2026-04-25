package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin.ChannelMessageBuildContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TextChannelMessagePlugin 契约测试。
 * 职责：验证文本消息插件的正文归一化与基础校验语义。
 * 边界：只验证 text 插件自身规则，不覆盖消息应用服务编排。
 */
@Tag("contract")
class TextChannelMessagePluginTests {

    /**
     * 验证文本消息插件会生成稳定正文与消息类型。
     */
    @Test
    @DisplayName("create message valid text draft builds normalized body")
    void createMessage_validTextDraft_buildsNormalizedBody() {
        TextChannelMessagePlugin plugin = new TextChannelMessagePlugin();

        ChannelMessage message = plugin.createMessage(
                new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                new TextChannelMessageDraft("  hello world  ")
        );

        assertEquals("text", message.messageType());
        assertEquals("hello world", message.body());
        assertEquals("hello world", message.previewText());
    }

    /**
     * 验证空白正文会返回稳定校验问题。
     */
    @Test
    @DisplayName("create message blank text draft throws validation problem")
    void createMessage_blankTextDraft_throwsValidationProblem() {
        TextChannelMessagePlugin plugin = new TextChannelMessagePlugin();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> plugin.createMessage(
                        new ChannelMessageBuildContext(5001L, "carrypigeon-local", 1L, 1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")),
                        new TextChannelMessageDraft("   ")
                )
        );

        assertEquals("body must not be blank", exception.getMessage());
    }
}
