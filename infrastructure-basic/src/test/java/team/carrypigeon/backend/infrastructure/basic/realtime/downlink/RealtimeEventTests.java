package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 验证实时事件的接收人规范化与必填约束。
 */
class RealtimeEventTests {

    /**
     * 验证接收人去空去重且顺序保持，避免同一账号重复投递。
     */
    @Test
    void constructor_duplicateAndNullRecipients_normalizes() {
        RealtimeEvent event = new RealtimeEvent(
                new RealtimeEventType("channel.changed"),
                Map.of(),
                Arrays.asList(2L, null, 2L, 1L, 1L)
        );

        assertEquals(List.of(2L, 1L), event.recipientAccountIds());
    }

    /**
     * 验证接收人为空时规范化为空列表，而不是 null。
     */
    @Test
    void constructor_nullRecipients_normalizesToEmptyList() {
        RealtimeEvent event = new RealtimeEvent(new RealtimeEventType("channel.changed"), Map.of(), null);

        assertTrue(event.recipientAccountIds().isEmpty());
    }

    /**
     * 验证接收人列表为不可变拷贝，发射方后续修改不影响已构造事件。
     */
    @Test
    void constructor_mutableRecipients_copiesImmutably() {
        List<Long> recipients = new ArrayList<>(List.of(1L));
        RealtimeEvent event = new RealtimeEvent(new RealtimeEventType("channel.changed"), Map.of(), recipients);

        recipients.add(2L);

        assertEquals(List.of(1L), event.recipientAccountIds());
        assertThrows(UnsupportedOperationException.class, () -> event.recipientAccountIds().add(3L));
    }

    /**
     * 验证缺少事件类型时快速失败。
     */
    @Test
    void constructor_nullType_failsFast() {
        assertThrows(NullPointerException.class, () -> new RealtimeEvent(null, Map.of(), List.of(1L)));
    }
}
