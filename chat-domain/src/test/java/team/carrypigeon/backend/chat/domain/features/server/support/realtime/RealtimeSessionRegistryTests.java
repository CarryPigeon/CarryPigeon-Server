package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
