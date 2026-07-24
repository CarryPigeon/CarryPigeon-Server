package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChannelAfterCommitPublisher 契约测试。
 * 职责：验证 channel feature 在事务提交后构造的 realtime 命令与通知偏好策略标记。
 * 边界：不验证 server feature 的过滤、缓存和 Netty 投递实现。
 */
@Tag("contract")
class ChannelAfterCommitPublisherTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");
    private static final AfterCommitExecutor DIRECT_AFTER_COMMIT = Runnable::run;

    /**
     * 验证已读状态事件仅面向当前账号，并应用通知偏好策略。
     */
    @Test
    @DisplayName("publish read state updated emits account scoped event")
    void publishReadStateUpdatedAfterCommit_readState_emitsAccountScopedEvent() {
        Fixture fixture = new Fixture();
        ChannelReadState readState = new ChannelReadState(
                9L, 1001L, 5001L, BASE_TIME.plusSeconds(5), BASE_TIME, BASE_TIME.plusSeconds(5));

        fixture.publisher.publishReadStateUpdatedAfterCommit(DIRECT_AFTER_COMMIT, readState);

        PublishRealtimeEventCommand command = fixture.singleCommand();
        JsonNode payload = fixture.payload(command);
        assertEquals(9L, command.channelId());
        assertEquals("read_state.updated", command.eventType());
        assertEquals(List.of(1001L), List.copyOf(command.recipientAccountIds()));
        assertTrue(command.applyNotificationPreferences());
        assertEquals("9", payload.path("cid").asText());
        assertEquals("1001", payload.path("uid").asText());
        assertEquals("5001", payload.path("last_read_mid").asText());
        assertEquals(BASE_TIME.plusSeconds(5).toEpochMilli(), payload.path("last_read_time").asLong());
    }

    /**
     * 验证频道刷新事件使用默认 scope，并应用通知偏好策略。
     */
    @Test
    @DisplayName("publish channel changed blank scope defaults to profile")
    void publishChannelChangedAfterCommit_blankScope_defaultsToProfile() {
        Fixture fixture = new Fixture();
        Channel channel = new Channel(9L, 9L, "general", "", "", "1001", "private", false,
                BASE_TIME, BASE_TIME);

        fixture.publisher.publishChannelChangedAfterCommit(
                DIRECT_AFTER_COMMIT, channel, " ", List.of(1001L, 1002L));

        PublishRealtimeEventCommand command = fixture.singleCommand();
        JsonNode payload = fixture.payload(command);
        assertEquals(9L, command.channelId());
        assertEquals("channel.changed", command.eventType());
        assertEquals(List.of(1001L, 1002L), List.copyOf(command.recipientAccountIds()));
        assertTrue(command.applyNotificationPreferences());
        assertEquals("9", payload.path("cid").asText());
        assertEquals("profile", payload.path("scope").asText());
        assertEquals("refresh", payload.path("hint").asText());
    }

    /**
     * 验证账号频道集合刷新不绑定单一频道，并绕过通知偏好过滤。
     */
    @Test
    @DisplayName("publish channels changed bypasses notification preferences")
    void publishChannelsChangedAfterCommit_account_bypassesNotificationPreferences() {
        Fixture fixture = new Fixture();

        fixture.publisher.publishChannelsChangedAfterCommit(DIRECT_AFTER_COMMIT, 1002L);

        PublishRealtimeEventCommand command = fixture.singleCommand();
        assertNull(command.channelId());
        assertEquals("channels.changed", command.eventType());
        assertEquals(List.of(1002L), List.copyOf(command.recipientAccountIds()));
        assertFalse(command.applyNotificationPreferences());
        assertEquals("refresh", fixture.payload(command).path("hint").asText());
    }

    /**
     * `Fixture` 测试夹具。
     * 职责：同步执行 after-commit 动作并记录跨 feature realtime 命令。
     */
    private static final class Fixture {

        private final JsonProvider jsonProvider = new JsonProvider(new ObjectMapper().findAndRegisterModules());
        private final RecordingRealtimeEventApi realtimeEventApi = new RecordingRealtimeEventApi();
        private final ChannelAfterCommitPublisher publisher = new ChannelAfterCommitPublisher(realtimeEventApi);

        private PublishRealtimeEventCommand singleCommand() {
            assertEquals(1, realtimeEventApi.commands.size());
            return realtimeEventApi.commands.getFirst();
        }

        private JsonNode payload(PublishRealtimeEventCommand command) {
            return jsonProvider.readTree(jsonProvider.toJson(command.payload()));
        }
    }

    /**
     * `RecordingRealtimeEventApi` 测试替身。
     * 职责：完整记录 channel feature 交给 server feature 的正式事件命令。
     */
    private static final class RecordingRealtimeEventApi implements RealtimeEventApi {

        private final List<PublishRealtimeEventCommand> commands = new ArrayList<>();

        @Override
        public void publish(PublishRealtimeEventCommand command) {
            commands.add(command);
        }
    }
}
