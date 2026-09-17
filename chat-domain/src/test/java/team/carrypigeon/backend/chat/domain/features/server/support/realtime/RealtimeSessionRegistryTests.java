package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.channel.Channel;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RealtimeSessionRegistry 契约测试。
 * 职责：验证断线续传事件窗口按账户隔离，避免其他账户高频事件挤掉当前账户锚点。
 * 边界：只验证单进程内存窗口行为，不覆盖跨实例或持久化回放。
 */
@Tag("contract")
class RealtimeSessionRegistryTests {

    /**
     * 验证 `eventsAfter` 在 `noisyOtherAccount` 条件下满足 `keepsCurrentAccountAnchor` 的测试契约。
     */
    @Test
    @DisplayName("events after noisy other account keeps current account anchor")
    void eventsAfter_noisyOtherAccount_keepsCurrentAccountAnchor() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        registry.appendEvent(RealtimeSessionRegistry.event("event-1", "message.created", 1L, java.util.Map.of("mid", "1"), List.of(1001L)));
        for (int index = 0; index < 250; index++) {
            registry.appendEvent(RealtimeSessionRegistry.event(
                    "noise-" + index,
                    "message.created",
                    10L + index,
                    java.util.Map.of("mid", Integer.toString(index)),
                    List.of(1002L)
            ));
        }
        registry.appendEvent(RealtimeSessionRegistry.event("event-2", "message.created", 300L, java.util.Map.of("mid", "2"), List.of(1001L)));

        List<RealtimeSessionRegistry.StoredRealtimeEvent> events = registry.eventsAfter(1001L, "event-1");

        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("event-2", events.getFirst().eventId());
    }

    /**
     * 验证单账号窗口超过 1000 条时只淘汰最旧事件，并保留其余锚点的续传顺序。
     */
    @Test
    @DisplayName("events after account window overflow evicts oldest event")
    void eventsAfter_accountWindowOverflow_evictsOldestEvent() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        for (int index = 0; index <= 1000; index++) {
            registry.appendEvent(RealtimeSessionRegistry.event(
                    "event-" + index,
                    "message.created",
                    index,
                    java.util.Map.of("mid", Integer.toString(index)),
                    List.of(1001L)
            ));
        }

        assertNull(registry.eventsAfter(1001L, "event-0"));
        List<RealtimeSessionRegistry.StoredRealtimeEvent> retained = registry.eventsAfter(1001L, "event-1");
        assertNotNull(retained);
        assertEquals(999, retained.size());
        assertEquals("event-2", retained.getFirst().eventId());
        assertEquals("event-1000", retained.getLast().eventId());
    }

    /**
     * 验证事件达到保留期后锚点失效，客户端可转入既有完整同步流程。
     */
    @Test
    @DisplayName("events after expired window returns unavailable anchor")
    void eventsAfter_expiredWindow_returnsUnavailableAnchor() {
        TimeProviderImpl timeProvider = mock(TimeProviderImpl.class);
        when(timeProvider.nowMillis()).thenReturn(0L, Duration.ofHours(1).toMillis());
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry(timeProvider, Duration.ofHours(1), 10);
        registry.appendEvent(RealtimeSessionRegistry.event(
                "event-1", "message.created", 0L, java.util.Map.of("mid", "1"), List.of(1001L)
        ));

        assertNull(registry.eventsAfter(1001L, "event-1"));
    }

    /**
     * 验证超过全局账号窗口上限时淘汰最后写入时间最早的账号。
     */
    @Test
    @DisplayName("append event account limit evicts oldest account window")
    void appendEvent_accountLimit_evictsOldestAccountWindow() {
        TimeProviderImpl timeProvider = mock(TimeProviderImpl.class);
        when(timeProvider.nowMillis()).thenReturn(100L, 200L, 300L);
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry(timeProvider, Duration.ofHours(1), 2);
        registry.appendEvent(event("event-1", 1001L));
        registry.appendEvent(event("event-2", 1002L));
        registry.appendEvent(event("event-3", 1003L));

        assertNull(registry.eventsAfter(1001L, "event-1"));
        assertNotNull(registry.eventsAfter(1002L, "event-2"));
        assertNotNull(registry.eventsAfter(1003L, "event-3"));
    }

    /**
     * 验证不可写通道被主动移除并关闭，避免写缓冲无限积压。
     */
    @Test
    @DisplayName("write text unwritable channel closes and unregisters")
    void writeText_unwritableChannel_closesAndUnregisters() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(false);
        registry.register(1001L, channel);

        registry.writeText(List.of(1001L), "payload");

        verify(channel).close();
        verify(channel, never()).writeAndFlush(any());
        assertTrue(registry.getChannels(1001L).isEmpty());
    }

    /**
     * 验证失效通道被移除且不会继续写入。
     */
    @Test
    @DisplayName("write text inactive channel skips and unregisters")
    void writeText_inactiveChannel_skipsAndUnregisters() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);
        registry.register(1001L, channel);

        registry.writeText(List.of(1001L), "payload");

        verify(channel, never()).writeAndFlush(any());
        verify(channel, never()).close();
        assertTrue(registry.getChannels(1001L).isEmpty());
    }

    private RealtimeSessionRegistry.StoredRealtimeEvent event(String eventId, long accountId) {
        return RealtimeSessionRegistry.event(
                eventId,
                "message.created",
                1L,
                java.util.Map.of("mid", eventId),
                List.of(accountId)
        );
    }
}
