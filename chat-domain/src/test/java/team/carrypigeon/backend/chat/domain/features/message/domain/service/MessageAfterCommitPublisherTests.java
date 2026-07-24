package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * MessageAfterCommitPublisher 契约测试。
 * 职责：验证 canonical 创建、撤回与 mention 事件只在 after-commit 阶段发布。
 */
@Tag("contract")
class MessageAfterCommitPublisherTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");
    private static final AfterCommitExecutor DIRECT = Runnable::run;

    /**
     * 验证 message.created 完整携带统一消息且没有专属顶层字段。
     */
    @Test
    @DisplayName("publish message created emits canonical message")
    void publishMessageCreatedAfterCommit_canonicalMessage_emitsCanonicalMessage() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Forward", "1.0.0",
                Map.of("forwarded_from", Map.of("mid", "5000")), BASE_TIME,
                List.of(1002L), "forward", MessageStatus.SENT
        );

        fixture.publisher.publishMessageCreatedAfterCommit(
                DIRECT, new AbstractMessageDomainSupport.PersistedMessage(message, List.of(1001L, 1002L), List.of())
        );

        PublishRealtimeEventCommand command = fixture.commands.getFirst();
        JsonNode published = fixture.payload(command).path("message");
        assertEquals("message.created", command.eventType());
        assertEquals("Core:Forward", published.path("domain").asText());
        assertEquals("5000", published.path("data").path("forwarded_from").path("mid").asText());
        assertEquals("1002", published.path("mentions").get(0).asText());
        assertFalse(published.has("forwarded_from"));
        assertFalse(published.has("sender"));
    }

    /**
     * 验证 message.recalled 使用精简生命周期载荷。
     */
    @Test
    @DisplayName("publish recalled emits compact payload")
    void publishMessageRecalledAfterCommit_recalledMessage_emitsCompactPayload() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Text", "1.0.0", Map.of(), BASE_TIME,
                List.of(), "消息已撤回", MessageStatus.RECALLED
        );

        fixture.publisher.publishMessageRecalledAfterCommit(
                DIRECT, new AbstractMessageDomainSupport.PersistedMessage(message, List.of(1001L), List.of())
        );

        PublishRealtimeEventCommand command = fixture.commands.getFirst();
        assertEquals("message.recalled", command.eventType());
        assertEquals("5001", fixture.payload(command).path("mid").asText());
        assertEquals(BASE_TIME.toEpochMilli(), fixture.payload(command).path("recall_time").asLong());
    }

    /**
     * 验证 mention.created 只投递给对应提醒用户。
     */
    @Test
    @DisplayName("publish mention targets mentioned user")
    void publishMessageCreatedAfterCommit_mention_targetsMentionedUser() {
        Fixture fixture = new Fixture();
        ChannelMessage message = new ChannelMessage(
                5001L, 1001L, 9L, "Core:Text", "1.0.0", Map.of("text", "hello"), BASE_TIME,
                List.of(1002L), "hello", MessageStatus.SENT
        );
        Mention mention = new Mention(8001L, 9L, 5001L, 1001L, "user", 1002L, BASE_TIME, false);

        fixture.publisher.publishMessageCreatedAfterCommit(
                DIRECT, new AbstractMessageDomainSupport.PersistedMessage(message, List.of(1001L, 1002L), List.of(mention))
        );

        PublishRealtimeEventCommand command = fixture.commands.get(1);
        assertEquals("mention.created", command.eventType());
        assertEquals(List.of(1002L), command.recipientAccountIds());
        assertEquals("1002", fixture.payload(command).path("uid").asText());
    }

    private static final class Fixture {

        private final JsonProvider jsonProvider = jsonProvider();
        private final List<PublishRealtimeEventCommand> commands = new ArrayList<>();
        private final MessageAfterCommitPublisher publisher = new MessageAfterCommitPublisher(
                (RealtimeEventApi) commands::add,
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC))
        );

        private JsonNode payload(PublishRealtimeEventCommand command) {
            return jsonProvider.readTree(jsonProvider.toJson(command.payload()));
        }
    }

    private static JsonProvider jsonProvider() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(mapper);
    }
}
