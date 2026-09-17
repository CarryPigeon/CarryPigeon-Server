package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.support.TestRealtimeDomainEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息读状态领域 API 契约测试。
 * 职责：验证消息归属、频道成员、读位置单调推进、未读映射和提交后发布语义。
 * 边界：使用内存仓储与事务替身，不连接数据库或启动 Spring 容器。
 */
@Tag("contract")
class MessageReadStateDomainApiTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");
    private static final long READ_TIME = BASE_TIME.toEpochMilli();

    /**
     * 验证首次更新会保存读状态，并在事务提交后发布当前账号的读状态事件。
     */
    @Test
    @DisplayName("update read state first position saves state and publishes event")
    void updateChannelReadState_firstPosition_savesStateAndPublishesEvent() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());
        fixture.messages.message = message(5001L, 9L);

        var result = fixture.service.updateChannelReadState(command(5001L));

        assertEquals("9", result.cid());
        assertEquals("1001", result.uid());
        assertEquals("5001", result.lastReadMid());
        assertEquals(READ_TIME, result.lastReadTime());
        assertEquals(1, fixture.readStates.upsertCount);
        verify(fixture.channelContextApi).requireMemberChannel(9L, 1001L);
        assertEquals("read_state.updated", fixture.events.commands.getFirst().eventType());
        assertEquals(List.of(1001L), fixture.events.commands.getFirst().recipientAccountIds());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) fixture.events.commands.getFirst().payload();
        assertEquals("5001", payload.get("last_read_mid"));
    }

    /**
     * 验证相同或更旧的消息位置不会覆盖当前状态，也不会重复发布事件。
     */
    @Test
    @DisplayName("update read state stale position keeps current state")
    void updateChannelReadState_stalePosition_keepsCurrentState() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());
        fixture.messages.message = message(5001L, 9L);
        fixture.readStates.current = new ChannelReadState(
                9L, 1001L, 5002L, BASE_TIME.plusSeconds(1), BASE_TIME.minusSeconds(10), BASE_TIME.plusSeconds(1)
        );

        var result = fixture.service.updateChannelReadState(command(5001L));

        assertEquals("5002", result.lastReadMid());
        assertEquals(0, fixture.readStates.upsertCount);
        assertEquals(0, fixture.events.commands.size());
    }

    /**
     * 验证不存在的消息不能成为读位置，失败语义保持为资源不存在。
     */
    @Test
    @DisplayName("update read state missing message throws not found")
    void updateChannelReadState_missingMessage_throwsNotFound() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.updateChannelReadState(command(5001L))
        );

        assertEquals("not_found", exception.reason());
        assertEquals("message does not exist", exception.getMessage());
        assertEquals(0, fixture.readStates.upsertCount);
    }

    /**
     * 验证其它频道的消息不能被用于推进当前频道的读位置。
     */
    @Test
    @DisplayName("update read state message in another channel throws not found")
    void updateChannelReadState_messageInAnotherChannel_throwsNotFound() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());
        fixture.messages.message = message(5001L, 10L);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.updateChannelReadState(command(5001L))
        );

        assertEquals("not_found", exception.reason());
        assertEquals(0, fixture.readStates.upsertCount);
    }

    /**
     * 验证频道成员校验失败时原始领域问题会直接传播，且不会写入读状态。
     */
    @Test
    @DisplayName("update read state non member propagates channel problem")
    void updateChannelReadState_nonMember_propagatesChannelProblem() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());
        fixture.messages.message = message(5001L, 9L);
        ProblemException forbidden = ProblemException.forbidden("channel_member_required", "channel member required");
        doThrow(forbidden).when(fixture.channelContextApi).requireMemberChannel(9L, 1001L);

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.updateChannelReadState(command(5001L))
        );

        assertSame(forbidden, exception);
        assertEquals(0, fixture.readStates.upsertCount);
    }

    /**
     * 验证未读仓储投影会稳定转换 ID，并把无读时间映射为零。
     */
    @Test
    @DisplayName("list unreads repository projection maps result")
    void listUnreads_repositoryProjection_mapsResult() {
        Fixture fixture = new Fixture(new ImmediateTransactionRunner());
        fixture.readStates.unreads = List.of(
                new ChannelUnread(9L, 3L, BASE_TIME),
                new ChannelUnread(10L, 7L, null)
        );

        var result = fixture.service.listUnreads(1001L);

        assertEquals("9", result.get(0).cid());
        assertEquals(3L, result.get(0).unreadCount());
        assertEquals(READ_TIME, result.get(0).lastReadTime());
        assertEquals(0L, result.get(1).lastReadTime());
        assertEquals(1001L, fixture.readStates.unreadAccountId);
    }

    /**
     * 验证实时事件在事务动作返回后仍未发布，只在显式提交阶段发出。
     */
    @Test
    @DisplayName("update read state before commit defers realtime event")
    void updateChannelReadState_beforeCommit_defersRealtimeEvent() {
        DeferredTransactionRunner transactionRunner = new DeferredTransactionRunner();
        Fixture fixture = new Fixture(transactionRunner);
        fixture.messages.message = message(5001L, 9L);

        fixture.service.updateChannelReadState(command(5001L));

        assertEquals(0, fixture.events.commands.size());
        transactionRunner.commit();
        assertEquals(1, fixture.events.commands.size());
    }

    /**
     * 验证事务回滚时已登记的读状态实时事件不会发出。
     */
    @Test
    @DisplayName("update read state rollback skips realtime event")
    void updateChannelReadState_rollback_skipsRealtimeEvent() {
        Fixture fixture = new Fixture(new RollbackingTransactionRunner());
        fixture.messages.message = message(5001L, 9L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fixture.service.updateChannelReadState(command(5001L))
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, fixture.events.commands.size());
    }

    private static UpdateChannelReadStateCommand command(long messageId) {
        return new UpdateChannelReadStateCommand(1001L, 9L, messageId, READ_TIME);
    }

    private static ChannelMessage message(long messageId, long channelId) {
        return new ChannelMessage(
                messageId, 1001L, channelId, "Core:Text", "1.0.0", Map.of("text", "hello"),
                BASE_TIME, List.of(), "hello", MessageStatus.SENT
        );
    }

    /**
     * `Fixture` 测试装配。
     * 职责：以最小依赖组合被测服务及可观察替身。
     */
    private static final class Fixture {

        private final InMemoryMessageRepository messages = new InMemoryMessageRepository();
        private final InMemoryReadStateRepository readStates = new InMemoryReadStateRepository();
        private final ChannelContextApi channelContextApi = mock(ChannelContextApi.class);
        private final RecordingRealtimeEventApi events = new RecordingRealtimeEventApi();
        private final MessageReadStateDomainApi service;

        private Fixture(TransactionRunner transactionRunner) {
            when(channelContextApi.requireMemberChannel(9L, 1001L))
                    .thenReturn(new ChannelMessagingContext(9L, 9L, "private"));
            service = new MessageReadStateDomainApi(
                    messages,
                    readStates,
                    channelContextApi,
                    transactionRunner,
                    new TestRealtimeDomainEventPublisher(events),
                    new TimeProviderImpl(Clock.fixed(BASE_TIME, ZoneOffset.UTC))
            );
        }
    }

    /**
     * `InMemoryMessageRepository` 测试替身。
     * 职责：只提供读状态用例所需的按 ID 查询能力。
     */
    private static final class InMemoryMessageRepository implements MessageRepository {

        private ChannelMessage message;

        @Override
        public ChannelMessage save(ChannelMessage message) {
            this.message = message;
            return message;
        }

        @Override
        public Optional<ChannelMessage> findById(long messageId) {
            return message == null || message.messageId() != messageId ? Optional.empty() : Optional.of(message);
        }

        @Override
        public java.util.Map<Long, ChannelMessage> findByIds(java.util.Collection<Long> messageIds) {
            if (message == null || !messageIds.contains(message.messageId())) {
                return java.util.Map.of();
            }
            return java.util.Map.of(message.messageId(), message);
        }

        @Override
        public ChannelMessage update(ChannelMessage message) {
            this.message = message;
            return message;
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return List.of();
        }

        @Override
        public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
            return List.of();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return List.of();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(
                long channelId,
                String keyword,
                Long cursorMessageId,
                Long senderAccountId,
                String domain,
                Long beforeMessageId,
                Long afterMessageId,
                int limit
        ) {
            return List.of();
        }
    }

    /**
     * `InMemoryReadStateRepository` 测试替身。
     * 职责：记录读状态写入并提供未读投影。
     */
    private static final class InMemoryReadStateRepository implements ChannelReadStateRepository {

        private ChannelReadState current;
        private List<ChannelUnread> unreads = List.of();
        private int upsertCount;
        private long unreadAccountId;

        @Override
        public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
            return Optional.ofNullable(current);
        }

        @Override
        public ChannelReadState upsert(ChannelReadState readState) {
            current = readState;
            upsertCount++;
            return readState;
        }

        @Override
        public boolean advanceIfNewer(ChannelReadState readState) {
            if (current != null && current.lastReadMessageId() >= readState.lastReadMessageId()) {
                return false;
            }
            upsert(readState);
            return true;
        }

        @Override
        public List<ChannelUnread> listUnreadsByAccountId(long accountId) {
            unreadAccountId = accountId;
            return unreads;
        }
    }

    /**
     * `RecordingRealtimeEventApi` 测试替身。
     * 职责：记录提交后发布的实时命令。
     */
    private static final class RecordingRealtimeEventApi implements RealtimeEventApi {

        private final List<PublishRealtimeEventCommand> commands = new ArrayList<>();

        @Override
        public void publish(PublishRealtimeEventCommand command) {
            commands.add(command);
        }
    }

    /**
     * `ImmediateTransactionRunner` 测试替身。
     * 职责：使用接口默认的提交后动作语义立即完成事务。
     */
    private static final class ImmediateTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    /**
     * `DeferredTransactionRunner` 测试替身。
     * 职责：分离事务动作与提交阶段，使测试可观察提交前状态。
     */
    private static final class DeferredTransactionRunner implements TransactionRunner {

        private final List<Runnable> afterCommitActions = new ArrayList<>();

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }

        @Override
        public <T> T runInTransaction(TransactionalAction<T> action) {
            return action.run(afterCommitActions::add);
        }

        private void commit() {
            List.copyOf(afterCommitActions).forEach(Runnable::run);
        }
    }

    /**
     * `RollbackingTransactionRunner` 测试替身。
     * 职责：在事务动作执行后模拟回滚，验证提交后动作不会执行。
     */
    private static final class RollbackingTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            action.get();
            throw new IllegalStateException("transaction rolled back");
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
            throw new IllegalStateException("transaction rolled back");
        }
    }
}
