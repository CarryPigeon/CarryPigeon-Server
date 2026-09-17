package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MessageMentionManager 契约测试。
 * 职责：验证一条消息派生的 mention 共享一致时间快照并批量持久化。
 * 边界：不验证数据库批量 INSERT，只验证 message feature 内部生成语义。
 */
@Tag("unit")
class MessageMentionManagerTests {

    /**
     * 验证多个合法 mention 只读取一次当前时间，并按消息声明顺序保存。
     */
    @Test
    @DisplayName("persist mentions multiple targets reuses one time snapshot")
    void persistMentions_multipleTargets_reusesOneTimeSnapshot() {
        MentionRepository mentionRepository = mock(MentionRepository.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.nextLongId()).thenReturn(7001L, 7002L);
        TimeProviderImpl timeProvider = mock(TimeProviderImpl.class);
        Instant createdAt = Instant.parse("2026-04-24T12:00:00Z");
        when(timeProvider.nowInstant()).thenReturn(createdAt);
        MessageMentionManager manager = new MessageMentionManager(mentionRepository, idGenerator, timeProvider);
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Text", "1.0.0", Map.of("text", "hello"),
                createdAt, List.of(1002L, 1003L), "hello", MessageStatus.SENT
        );

        List<Mention> result = manager.persistMentions(message, List.of(1001L, 1002L, 1003L));

        assertEquals(List.of(7001L, 7002L), result.stream().map(Mention::mentionId).toList());
        assertEquals(List.of(createdAt, createdAt), result.stream().map(Mention::createdAt).toList());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mention>> captor = ArgumentCaptor.forClass(List.class);
        verify(mentionRepository).saveAll(captor.capture());
        assertEquals(List.of(1002L, 1003L), captor.getValue().stream().map(Mention::targetAccountId).toList());
        verify(timeProvider, times(1)).nowInstant();
    }
}
