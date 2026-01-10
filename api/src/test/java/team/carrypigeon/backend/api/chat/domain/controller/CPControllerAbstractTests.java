package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CPControllerAbstractTests {

    public static class VO {
        public int x;
    }

    private static CPSession session() {
        return new CPSession() {
            @Override
            public void write(String msg, boolean encrypted) {
            }

            @Override
            public <T> T getAttributeValue(String key, Class<T> type) {
                return null;
            }

            @Override
            public void setAttributeValue(String key, Object value) {
            }

            @Override
            public void close() {
            }
        };
    }

    @Test
    void process_whenJsonMappingFails_shouldReturnError100() {
        ObjectMapper mapper = new ObjectMapper();
        CPControllerAbstract<VO> controller = new CPControllerAbstract<>(mapper, VO.class) {
            @Override
            protected CPResponse check(CPSession session, VO data, Map<String, Object> context) {
                return null;
            }

            @Override
            protected CPResponse process0(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.success();
            }
        };

        JsonNode bad = JsonNodeFactory.instance.objectNode().put("x", "not-int");
        CPResponse resp = controller.process(session(), bad);
        assertEquals(100, resp.getCode());
        assertEquals("error parsing request data", resp.getData().get("msg").asText());
    }

    @Test
    void process_whenCheckReturnsNonNull_shouldReturnCheckResponse() {
        ObjectMapper mapper = new ObjectMapper();
        CPControllerAbstract<VO> controller = new CPControllerAbstract<>(mapper, VO.class) {
            @Override
            protected CPResponse check(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.authorityError("no");
            }

            @Override
            protected CPResponse process0(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.success();
            }
        };

        CPResponse resp = controller.process(session(), JsonNodeFactory.instance.objectNode().put("x", 1));
        assertEquals(300, resp.getCode());
        assertEquals("no", resp.getData().get("msg").asText());
    }

    @Test
    void process_whenSuccess_shouldCallNotify() {
        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean notified = new AtomicBoolean(false);
        CPControllerAbstract<VO> controller = new CPControllerAbstract<>(mapper, VO.class) {
            @Override
            protected CPResponse check(CPSession session, VO data, Map<String, Object> context) {
                return null;
            }

            @Override
            protected CPResponse process0(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.success().setTextData("ok");
            }

            @Override
            protected void notify(CPSession session, VO data, Map<String, Object> context) {
                notified.set(true);
            }
        };

        CPResponse resp = controller.process(session(), JsonNodeFactory.instance.objectNode().put("x", 1));
        assertEquals(200, resp.getCode());
        assertTrue(notified.get());
    }

    @Test
    void process_whenNotSuccess_shouldNotCallNotify() {
        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean notified = new AtomicBoolean(false);
        CPControllerAbstract<VO> controller = new CPControllerAbstract<>(mapper, VO.class) {
            @Override
            protected CPResponse check(CPSession session, VO data, Map<String, Object> context) {
                return null;
            }

            @Override
            protected CPResponse process0(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.error("bad");
            }

            @Override
            protected void notify(CPSession session, VO data, Map<String, Object> context) {
                notified.set(true);
            }
        };

        CPResponse resp = controller.process(session(), JsonNodeFactory.instance.objectNode().put("x", 1));
        assertEquals(100, resp.getCode());
        assertFalse(notified.get());
    }

    @Test
    void process_whenSuccessWithoutNotifyOverride_shouldStillSucceed() {
        ObjectMapper mapper = new ObjectMapper();
        CPControllerAbstract<VO> controller = new CPControllerAbstract<>(mapper, VO.class) {
            @Override
            protected CPResponse check(CPSession session, VO data, Map<String, Object> context) {
                return null;
            }

            @Override
            protected CPResponse process0(CPSession session, VO data, Map<String, Object> context) {
                return CPResponse.success();
            }
        };

        CPResponse resp = controller.process(session(), JsonNodeFactory.instance.objectNode().put("x", 1));
        assertEquals(200, resp.getCode());
    }
}
