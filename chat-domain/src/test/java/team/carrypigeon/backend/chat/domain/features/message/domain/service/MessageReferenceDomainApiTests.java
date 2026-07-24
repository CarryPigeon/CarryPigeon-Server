package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MessageReferenceDomainApi 契约测试。
 * 职责：验证跨 feature 消息引用查询只暴露消息 ID、频道 ID 和存在性。
 * 边界：不暴露正文、发送者或消息仓储实现。
 */
@Tag("contract")
class MessageReferenceDomainApiTests {

    /**
     * 验证已存在消息会被收敛为最小引用投影。
     */
    @Test
    @DisplayName("require message existing message returns reference projection")
    void requireMessage_existingMessage_returnsReferenceProjection() {
        MessageRepository repository = mock(MessageRepository.class);
        when(repository.findById(5001L)).thenReturn(Optional.of(message(5001L, 1L)));
        MessageReferenceDomainApi api = new MessageReferenceDomainApi(repository);

        MessageReferenceResult result = api.requireMessage(5001L);

        assertEquals(5001L, result.messageId());
        assertEquals(1L, result.channelId());
    }

    /**
     * 验证消息不存在时保持稳定的 not found 领域问题。
     */
    @Test
    @DisplayName("require message missing message throws not found problem")
    void requireMessage_missingMessage_throwsNotFoundProblem() {
        MessageRepository repository = mock(MessageRepository.class);
        when(repository.findById(5001L)).thenReturn(Optional.empty());
        MessageReferenceDomainApi api = new MessageReferenceDomainApi(repository);

        ProblemException exception = assertThrows(ProblemException.class, () -> api.requireMessage(5001L));

        assertEquals("not_found", exception.reason());
    }

    /**
     * 验证频道存在任意消息时只进行单条存在性查询并返回 true。
     */
    @Test
    @DisplayName("has channel messages existing message returns true")
    void hasChannelMessages_existingMessage_returnsTrue() {
        MessageRepository repository = mock(MessageRepository.class);
        when(repository.findByChannelIdBefore(1L, null, 1)).thenReturn(List.of(message(5001L, 1L)));
        MessageReferenceDomainApi api = new MessageReferenceDomainApi(repository);

        assertTrue(api.hasChannelMessages(1L));
    }

    private ChannelMessage message(long messageId, long channelId) {
        return new ChannelMessage(
                messageId,
                1001L,
                channelId,
                "Core:Text",
                "1.0.0",
                java.util.Map.of("text", "hello"),
                Instant.parse("2026-07-17T12:00:00Z"),
                List.of(),
                "hello",
                team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus.SENT
        );
    }
}
