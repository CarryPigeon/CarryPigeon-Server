package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息转发契约测试。
 * 职责：验证单条和合并转发关系只进入 Core:Forward.data。
 */
@Tag("contract")
class ChannelMessagePublishingDomainApiForwardTests {

    /**
     * 验证单条转发使用源消息真实 canonical 元数据构造快照。
     */
    @Test
    @DisplayName("forward single message stores source in data")
    void forwardChannelMessage_singleMessage_storesSourceInData() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();

        ChannelMessageResult result = fixture.publishingApi.forwardChannelMessage(
                new ForwardChannelMessageCommand(1001L, 5001L, 1L, "FYI", List.of(), null)
        );

        assertEquals("Core:Forward", result.domain());
        assertEquals("FYI", result.preview());
        assertTrue(result.data().containsKey("forwarded_from"));
        assertEquals(Map.of("text", "FYI"), result.data().get("content"));
        assertTrue(!result.data().containsKey("forwarded_messages"));
    }

    /**
     * 验证合并转发保持请求顺序并保留不可用来源占位。
     */
    @Test
    @DisplayName("forward merged messages preserves order and unavailable source")
    void forwardChannelMessage_mergedMessages_preservesOrderAndUnavailableSource() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();

        ChannelMessageResult result = fixture.publishingApi.forwardChannelMessage(
                new ForwardChannelMessageCommand(1001L, 5001L, 1L, null, List.of(5002L, 5999L, 5001L), null)
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) result.data().get("forwarded_messages");
        assertEquals("5002", sources.get(0).get("mid"));
        assertEquals("5999", sources.get(1).get("mid"));
        assertEquals(true, sources.get(1).get("unavailable"));
        assertEquals("5001", sources.get(2).get("mid"));
        assertEquals("转发 3 条消息", result.preview());
    }

    /**
     * 验证单项 merged_mids 被拒绝。
     */
    @Test
    @DisplayName("forward merged single id fails validation")
    void forwardChannelMessage_singleMergedId_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.forwardChannelMessage(
                        new ForwardChannelMessageCommand(1001L, 5001L, 1L, null, List.of(5001L), null)
                )
        );

        assertEquals("merged_mids must contain at least two ids", exception.getMessage());
    }

    /**
     * 验证同一幂等键和等价请求返回首次消息且不重复写入或推送事件。
     */
    @Test
    @DisplayName("forward same idempotency key returns original result")
    void forwardChannelMessage_sameIdempotencyKey_returnsOriginalResultOnce() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();
        ForwardChannelMessageCommand command = new ForwardChannelMessageCommand(
                1001L, 5001L, 1L, " FYI ", List.of(), " forward-1 "
        );

        ChannelMessageResult first = fixture.publishingApi.forwardChannelMessage(command);
        ChannelMessageResult second = fixture.publishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                1001L, 5001L, 1L, "FYI", List.of(), "forward-1"
        ));

        assertEquals(first, second);
        assertEquals(1, fixture.messageRepository.savedMessages.size());
        assertEquals(1, fixture.publisher.publishedMessages.size());
        assertEquals(1, fixture.messageIdempotencyRepository.reservations.size());
    }

    /**
     * 验证同一幂等键不能绑定到内容不同的转发请求。
     */
    @Test
    @DisplayName("forward reused idempotency key with different request conflicts")
    void forwardChannelMessage_reusedIdempotencyKeyDifferentRequest_conflicts() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();
        fixture.publishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                1001L, 5001L, 1L, "first", List.of(), "forward-1"
        ));

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                        1001L, 5001L, 1L, "second", List.of(), "forward-1"
                ))
        );

        assertEquals("idempotency_key_reused", exception.reason());
        assertEquals(1, fixture.messageRepository.savedMessages.size());
        assertEquals(1, fixture.publisher.publishedMessages.size());
    }

    /**
     * 验证缺少幂等键时每次调用仍创建并推送一条新消息。
     */
    @Test
    @DisplayName("forward without idempotency key creates every request")
    void forwardChannelMessage_withoutIdempotencyKey_createsEveryRequest() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();
        ForwardChannelMessageCommand command = new ForwardChannelMessageCommand(
                1001L, 5001L, 1L, "FYI", List.of(), null
        );

        fixture.publishingApi.forwardChannelMessage(command);
        fixture.publishingApi.forwardChannelMessage(command);

        assertEquals(2, fixture.messageRepository.savedMessages.size());
        assertEquals(2, fixture.publisher.publishedMessages.size());
        assertTrue(fixture.messageIdempotencyRepository.reservations.isEmpty());
    }

    /**
     * 验证超长幂等键会在持久化前被拒绝。
     */
    @Test
    @DisplayName("forward oversized idempotency key fails validation")
    void forwardChannelMessage_oversizedIdempotencyKey_failsValidation() {
        MessageDomainApiTestSupport.Fixture fixture = fixtureWithSources();

        ProblemException exception = assertThrows(ProblemException.class, () ->
                fixture.publishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                        1001L, 5001L, 1L, null, List.of(), "x".repeat(129)
                ))
        );

        assertEquals("idempotency key length must be less than or equal to 128", exception.getMessage());
        assertTrue(fixture.messageIdempotencyRepository.reservations.isEmpty());
    }

    /**
     * 验证事务锁串行化相同幂等键时，并发调用只创建一条消息。
     */
    @Test
    @DisplayName("forward concurrent same key creates one message")
    void forwardChannelMessage_concurrentSameKey_createsOneMessage() throws Exception {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(
                null,
                new SynchronizedTransactionRunner()
        );
        fixture.messageRepository.messagesById.put(5001L, source(5001L, 1002L, "first"));
        ForwardChannelMessageCommand command = new ForwardChannelMessageCommand(
                1001L, 5001L, 1L, "FYI", List.of(), "forward-concurrent"
        );
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<ChannelMessageResult> first = CompletableFuture.supplyAsync(
                    () -> fixture.publishingApi.forwardChannelMessage(command), executor
            );
            CompletableFuture<ChannelMessageResult> second = CompletableFuture.supplyAsync(
                    () -> fixture.publishingApi.forwardChannelMessage(command), executor
            );

            assertEquals(first.get(), second.get());
            assertEquals(1, fixture.messageRepository.savedMessages.size());
            assertEquals(1, fixture.publisher.publishedMessages.size());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 验证幂等结果绑定失败时消息、预留和 realtime 副作用整体回滚。
     */
    @Test
    @DisplayName("forward idempotency completion failure rolls back transaction")
    void forwardChannelMessage_idempotencyCompletionFailure_rollsBackTransaction() {
        SnapshotTransactionRunner transactionRunner = new SnapshotTransactionRunner();
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null, transactionRunner);
        fixture.messageRepository.messagesById.put(5001L, source(5001L, 1002L, "first"));
        transactionRunner.bind(fixture);
        fixture.messageIdempotencyRepository.failOnComplete = true;

        assertThrows(IllegalStateException.class, () -> fixture.publishingApi.forwardChannelMessage(
                new ForwardChannelMessageCommand(1001L, 5001L, 1L, "FYI", List.of(), "forward-rollback")
        ));

        assertTrue(fixture.messageRepository.savedMessages.isEmpty());
        assertEquals(1, fixture.messageRepository.messagesById.size());
        assertTrue(fixture.messageIdempotencyRepository.reservations.isEmpty());
        assertTrue(fixture.publisher.publishedMessages.isEmpty());
    }

    private MessageDomainApiTestSupport.Fixture fixtureWithSources() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.messagesById.put(5001L, source(5001L, 1002L, "first"));
        fixture.messageRepository.messagesById.put(5002L, source(5002L, 1002L, "second"));
        return fixture;
    }

    private ChannelMessage source(long messageId, long senderId, String text) {
        return new ChannelMessage(
                messageId, senderId, 1L, "Core:Text", "1.0.0", Map.of("text", text),
                MessageDomainApiTestSupport.BASE_TIME, List.of(), text, MessageStatus.SENT
        );
    }

    /**
     * 串行事务测试替身，用于模拟数据库唯一键与行锁形成的临界区。
     */
    private static final class SynchronizedTransactionRunner implements TransactionRunner {

        @Override
        public synchronized <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public synchronized void runInTransaction(Runnable action) {
            action.run();
        }
    }

    /**
     * 可回滚事务测试替身，用于验证幂等绑定与消息写入属于同一事务。
     */
    private static final class SnapshotTransactionRunner implements TransactionRunner {

        private MessageDomainApiTestSupport.Fixture fixture;

        private void bind(MessageDomainApiTestSupport.Fixture fixture) {
            this.fixture = fixture;
        }

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            List<ChannelMessage> savedSnapshot = new ArrayList<>(fixture.messageRepository.savedMessages);
            Map<Long, ChannelMessage> messageSnapshot = new HashMap<>(fixture.messageRepository.messagesById);
            Map<String, team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency>
                    idempotencySnapshot = new HashMap<>(fixture.messageIdempotencyRepository.reservations);
            try {
                return action.get();
            } catch (RuntimeException exception) {
                fixture.messageRepository.savedMessages.clear();
                fixture.messageRepository.savedMessages.addAll(savedSnapshot);
                fixture.messageRepository.messagesById.clear();
                fixture.messageRepository.messagesById.putAll(messageSnapshot);
                fixture.messageIdempotencyRepository.reservations.clear();
                fixture.messageIdempotencyRepository.reservations.putAll(idempotencySnapshot);
                throw exception;
            }
        }

        @Override
        public void runInTransaction(Runnable action) {
            runInTransaction(() -> {
                action.run();
                return null;
            });
        }
    }
}
