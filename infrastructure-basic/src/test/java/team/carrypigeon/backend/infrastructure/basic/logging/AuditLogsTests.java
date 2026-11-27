package team.carrypigeon.backend.infrastructure.basic.logging;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** 验证统一日志的上下文、事件字段及逐行 JSON 契约。 */
class AuditLogsTests {
    @Test
    void emitsOneJsonRecordWithEventFields() throws Exception {
        var context = new SortedArrayStringMap();
        context.putValue("action", "auth.login");
        var event = Log4jLogEvent.newBuilder().setContextData(context)
                .setMessage(new SimpleMessage("first\nsecond")).build();
        String json = JsonLayout.newBuilder().setCompact(true).setEventEol(true).setProperties(true).build()
                .toSerializable(event);
        assertEquals(1, json.lines().count());
        assertEquals("auth.login", new ObjectMapper().readTree(json).at("/contextMap/action").asText());
    }

    @Test
    void propagatesOnlyOperationFieldsAndRestoresWorker() {
        Runnable task;
        try (var scope = LogContexts.openScope(LogScope.HTTP)) {
            LogContexts.uid("42");
            LogContexts.put("action", "auth.login");
            task = LogContexts.wrap(LogScope.WS, () -> {
                assertEquals("42", MDC.get("uid"));
                assertEquals("ws", LogContexts.currentScope());
                assertNull(MDC.get("action"));
            });
        }
        try (var scope = LogContexts.openScope()) {
            LogContexts.uid("7");
            task.run();
            assertEquals("7", MDC.get("uid"));
            LogContexts.uid(null);
            assertNull(MDC.get("uid"));
        }
    }

    @Test
    void boundsExternalCorrelationIds() {
        assertTrue(LogContexts.usableId("req-1"));
        assertFalse(LogContexts.usableId(null));
        assertFalse(LogContexts.usableId("x\ny"));
        assertFalse(LogContexts.usableId("x".repeat(129)));
    }
}
