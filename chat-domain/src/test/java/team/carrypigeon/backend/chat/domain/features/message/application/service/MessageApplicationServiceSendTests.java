package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageApplicationService 发送契约测试。
 * 职责：验证文本消息发送入口的应用层编排契约与权限失败语义。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证发送相关语义。
 */
@Tag("contract")
class MessageApplicationServiceSendTests {

    /**
     * 验证发送文本消息时会持久化并使用同一个 messageId 进行实时分发。
     */
    @Test
    @DisplayName("send channel text message valid command persists and publishes same message id")
    void sendChannelTextMessage_validCommand_persistsAndPublishesSameMessageId() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelTextMessage(
                new SendChannelTextMessageCommand(1001L, 1L, "hello world")
        );

        assertEquals(5001L, result.messageId());
        assertEquals(5001L, fixture.messageRepository.savedMessages.getFirst().messageId());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertEquals("carrypigeon-local", result.serverId());
        assertEquals("hello world", result.body());
        assertEquals("hello world", result.previewText());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证通用消息发送入口在 text 草稿场景下保持既有 text 消息语义。
     */
    @Test
    @DisplayName("send channel message text draft preserves text semantics")
    void sendChannelMessage_textDraft_preservesTextSemantics() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelMessage(
                new SendChannelMessageCommand(1001L, 1L, new TextChannelMessageDraft("hello plugin world"))
        );

        assertEquals("text", result.messageType());
        assertEquals("hello plugin world", result.body());
        assertEquals("hello plugin world", result.previewText());
        assertEquals(null, result.payload());
        assertEquals(null, result.metadata());
        assertEquals("sent", result.status());
    }

    /**
     * 验证非成员发送消息时会返回权限问题语义。
     */
    @Test
    @DisplayName("send channel text message non member throws forbidden problem")
    void sendChannelTextMessage_nonMember_throwsForbiddenProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }
}
