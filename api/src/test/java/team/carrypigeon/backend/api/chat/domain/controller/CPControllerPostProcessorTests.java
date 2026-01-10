package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

import static org.junit.jupiter.api.Assertions.*;

class CPControllerPostProcessorTests {

    static class ValidVo implements CPControllerVO {
        @Override
        public boolean insertData(CPFlowContext context) {
            return true;
        }
    }

    static class ValidResult implements CPControllerResult {
        @Override
        public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        }
    }

    static class InvalidVo {
    }

    static class InvalidResult {
    }

    @CPControllerTag(path = "/core/valid", voClazz = ValidVo.class, resultClazz = ValidResult.class)
    static class ValidController implements CPController {
        @Override
        public CPResponse process(CPSession session, JsonNode data) {
            return CPResponse.success();
        }
    }

    @CPControllerTag(path = "/core/invalid", voClazz = InvalidVo.class, resultClazz = InvalidResult.class)
    static class InvalidController {
    }

    static class PlainBean {
    }

    @Test
    void postProcessAfterInitialization_shouldRegisterValidControllerOnly() {
        CPControllerPostProcessor processor = new CPControllerPostProcessor();

        processor.postProcessAfterInitialization(new PlainBean(), "plainBean");
        assertTrue(processor.getControllerAndVoMap().isEmpty());
        assertTrue(processor.getControllerAndResultMap().isEmpty());

        processor.postProcessAfterInitialization(new InvalidController(), "invalidController");
        assertTrue(processor.getControllerAndVoMap().isEmpty());
        assertTrue(processor.getControllerAndResultMap().isEmpty());

        processor.postProcessAfterInitialization(new ValidController(), "validController");
        assertEquals(1, processor.getControllerAndVoMap().size());
        assertEquals(1, processor.getControllerAndResultMap().size());
        assertEquals(ValidVo.class, processor.getControllerAndVoMap().get("/core/valid"));
        assertEquals(ValidResult.class, processor.getControllerAndResultMap().get("/core/valid"));
    }
}

