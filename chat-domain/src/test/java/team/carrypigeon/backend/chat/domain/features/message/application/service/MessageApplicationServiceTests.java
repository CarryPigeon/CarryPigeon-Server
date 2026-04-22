package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageApplicationService 契约测试。
 * 职责：验证频道消息发送与历史查询用例的应用层编排契约。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证业务语义。
 */
class MessageApplicationServiceTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");

    /**
     * 验证发送文本消息时会持久化并使用同一个 messageId 进行实时分发。
     */
    @Test
    @DisplayName("send channel text message valid command persists and publishes same message id")
    void sendChannelTextMessage_validCommand_persistsAndPublishesSameMessageId() {
        Fixture fixture = new Fixture();

        ChannelMessageResult result = fixture.service.sendChannelTextMessage(
                new SendChannelTextMessageCommand(1001L, 1L, "hello world")
        );

        assertEquals(5001L, result.messageId());
        assertEquals(5001L, fixture.messageRepository.savedMessages.getFirst().messageId());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertEquals("carrypigeon-local", result.serverId());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证非成员发送消息时会返回权限问题语义。
     */
    @Test
    @DisplayName("send channel text message non member throws forbidden problem")
    void sendChannelTextMessage_nonMember_throwsForbiddenProblem() {
        Fixture fixture = new Fixture();
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证按频道查询历史消息时会返回倒序消息和下一页游标。
     */
    @Test
    @DisplayName("get channel message history member returns messages and next cursor")
    void getChannelMessageHistory_member_returnsMessagesAndNextCursor() {
        Fixture fixture = new Fixture();
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L, "carrypigeon-local", 1L, 1L, 1002L, "text", "second", null, null, "sent", BASE_TIME.plusSeconds(1)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5001L, "carrypigeon-local", 1L, 1L, 1001L, "text", "first", null, null, "sent", BASE_TIME
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 20)
        );

        assertEquals(2, result.messages().size());
        assertEquals(5002L, result.messages().getFirst().messageId());
        assertEquals(5001L, result.nextCursor());
    }

    /**
     * 验证频道不存在时历史查询会返回不存在问题语义。
     */
    @Test
    @DisplayName("get channel message history missing channel throws not found problem")
    void getChannelMessageHistory_missingChannel_throwsNotFoundProblem() {
        Fixture fixture = new Fixture();
        fixture.channelRepository.channel = null;

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getChannelMessageHistory(new GetChannelMessageHistoryQuery(1001L, 9L, null, 20))
        );

        assertEquals("channel does not exist", exception.getMessage());
    }

    private static class Fixture {

        private final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        private final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        private final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        private final RecordingMessageRealtimePublisher publisher = new RecordingMessageRealtimePublisher();
        private final MessageApplicationService service = new MessageApplicationService(
                channelRepository,
                channelMemberRepository,
                messageRepository,
                publisher,
                new ServerIdentityProperties("carrypigeon-local"),
                new FixedIdGenerator(),
                new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                new NoopTransactionRunner()
        );

        private Fixture() {
            channelRepository.channel = new Channel(1L, 1L, "public", "public", true, BASE_TIME, BASE_TIME);
            channelMemberRepository.memberships.put(1L, List.of(1001L, 1002L));
        }
    }

    private static class InMemoryChannelRepository implements ChannelRepository {

        private Channel channel;

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channel);
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channel != null && channel.id() == channelId ? channel : null);
        }
    }

    private static class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        private final Map<Long, List<Long>> memberships = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return memberships.getOrDefault(channelId, List.of()).contains(accountId);
        }

        @Override
        public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
            memberships.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>()).add(channelMember.accountId());
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return memberships.getOrDefault(channelId, List.of());
        }
    }

    private static class InMemoryMessageRepository implements MessageRepository {

        private final List<ChannelMessage> savedMessages = new ArrayList<>();
        private final List<ChannelMessage> history = new ArrayList<>();

        @Override
        public ChannelMessage save(ChannelMessage message) {
            savedMessages.add(message);
            return message;
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return history;
        }
    }

    private static class RecordingMessageRealtimePublisher implements MessageRealtimePublisher {

        private final List<ChannelMessage> publishedMessages = new ArrayList<>();
        private final List<List<Long>> recipientAccountIds = new ArrayList<>();

        @Override
        public void publish(ChannelMessage message, java.util.Collection<Long> recipients) {
            publishedMessages.add(message);
            recipientAccountIds.add(List.copyOf(recipients));
        }
    }

    private static class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 5001L;
        }
    }

    private static class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }
}
