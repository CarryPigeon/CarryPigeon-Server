package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 消息提交后事件发布契约测试。
 * 职责：验证 message feature 发布 canonical 消息、撤回与 mention 事实，不构造 server 命令。
 */
@Tag("contract")
class MessageAfterCommitPublisherTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");
    private static final AfterCommitExecutor DIRECT = Runnable::run;

    /**
     * 验证创建消息事件携带完整 canonical 字段和接收者快照。
     */
    @Test
    @DisplayName("publish message created emits canonical fact")
    void publishMessageCreatedAfterCommit_canonicalMessage_emitsCanonicalFact() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Forward", "1.0.0",
                Map.of("forwarded_from", Map.of("mid", "5000")), BASE_TIME,
                List.of(1002L), "forward", MessageStatus.SENT
        );

        fixture.publisher.publishMessageCreatedAfterCommit(
                DIRECT, message, List.of(1001L, 1002L), List.of()
        );

        MessageCreatedEvent event = assertInstanceOf(MessageCreatedEvent.class, fixture.events.getFirst());
        assertEquals("Core:Forward", event.domain());
        assertEquals(Map.of("mid", "5000"), event.data().get("forwarded_from"));
        assertEquals(List.of(1002L), event.mentions());
        assertEquals("sent", event.status());
        assertEquals(List.of(1001L, 1002L), event.recipientAccountIds());
    }

    /**
     * 验证撤回事实包含精确发生时间和接收者快照。
     */
    @Test
    @DisplayName("publish recalled emits compact fact")
    void publishMessageRecalledAfterCommit_recalledMessage_emitsCompactFact() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Text", "1.0.0", Map.of(), BASE_TIME,
                List.of(), "消息已撤回", MessageStatus.RECALLED
        );

        fixture.publisher.publishMessageRecalledAfterCommit(
                DIRECT, message, List.of(1001L)
        );

        MessageRecalledEvent event = assertInstanceOf(MessageRecalledEvent.class, fixture.events.getFirst());
        assertEquals(5001L, event.messageId());
        assertEquals(BASE_TIME.toEpochMilli(), event.recallTime());
        assertEquals(List.of(1001L), event.recipientAccountIds());
    }

    /**
     * 验证每条 mention 都发布独立事实，并保留目标账号。
     */
    @Test
    @DisplayName("publish message created mention emits mention fact")
    void publishMessageCreatedAfterCommit_mention_emitsMentionFact() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Text", "1.0.0", Map.of("text", "hello"), BASE_TIME,
                List.of(1002L), "hello", MessageStatus.SENT
        );
        Mention mention = new Mention(8001L, 9L, 5001L, 1001L, "user", 1002L, BASE_TIME, false);

        fixture.publisher.publishMessageCreatedAfterCommit(
                DIRECT, message, List.of(1001L, 1002L), List.of(mention)
        );

        MentionCreatedEvent event = assertInstanceOf(MentionCreatedEvent.class, fixture.events.get(1));
        assertEquals(1002L, event.targetAccountId());
        assertEquals(8001L, event.mentionId());
    }

    private static final class Fixture {

        private final List<Object> events = new ArrayList<>();
        private final MessageAfterCommitPublisher publisher = new MessageAfterCommitPublisher(
                events::add,
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC))
        );
    }
}
