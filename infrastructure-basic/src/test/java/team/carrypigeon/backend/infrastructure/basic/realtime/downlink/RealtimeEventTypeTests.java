package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 验证实时事件类型只接受合法的线上事件名。
 */
class RealtimeEventTypeTests {

    /**
     * 验证合法事件名被接受。
     */
    @Test
    void constructor_validWireName_accepts() {
        assertDoesNotThrow(() -> new RealtimeEventType("read_state.updated"));
    }

    /**
     * 验证空、空白事件名快速失败。
     */
    @Test
    void constructor_blankWireName_failsFast() {
        assertThrows(IllegalArgumentException.class, () -> new RealtimeEventType(null));
        assertThrows(IllegalArgumentException.class, () -> new RealtimeEventType("   "));
    }

    /**
     * 验证大写或含空白的非法事件名快速失败，避免破坏线上事件名规则。
     */
    @Test
    void constructor_illegalWireName_failsFast() {
        assertThrows(IllegalArgumentException.class, () -> new RealtimeEventType("Message.Created"));
        assertThrows(IllegalArgumentException.class, () -> new RealtimeEventType("message created"));
    }
}
