package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiWsEventStoreTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resumeAfter_beforeEarliestEvent_expectedEventTooOld() {
        ApiWsEventStore store = new ApiWsEventStore(new CpApiProperties());
        store.append(new ApiWsEventStore.StoredEvent(
                "100",
                100L,
                "message.created",
                System.currentTimeMillis(),
                objectMapper.createObjectNode().put("cid", "1")
        ));

        ApiWsEventStore.ResumeResult result = store.resumeAfter("99", 100);

        assertEquals(CPProblemReason.EVENT_TOO_OLD.code(), result.failedReason());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void resumeAfter_validAnchor_expectedReplayedEvents() {
        ApiWsEventStore store = new ApiWsEventStore(new CpApiProperties());
        store.append(new ApiWsEventStore.StoredEvent(
                "100",
                100L,
                "message.created",
                System.currentTimeMillis(),
                objectMapper.createObjectNode().put("cid", "1")
        ));
        store.append(new ApiWsEventStore.StoredEvent(
                "101",
                101L,
                "message.deleted",
                System.currentTimeMillis(),
                objectMapper.createObjectNode().put("cid", "1")
        ));

        ApiWsEventStore.ResumeResult result = store.resumeAfter("100", 100);

        assertNull(result.failedReason());
        assertEquals(1, result.events().size());
        assertEquals("101", result.events().getFirst().eventId());
    }
}
