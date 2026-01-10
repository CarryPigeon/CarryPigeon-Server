package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

import static org.junit.jupiter.api.Assertions.*;

class CPControllerResultTests {

    @Test
    void argsError_shouldWriteStandardErrorResponseToContext() {
        CPControllerResult result = new CPControllerResult() {
            @Override
            public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
            }
        };

        CPFlowContext context = new CPFlowContext();
        result.argsError(context);

        Object value = context.getData("response");
        assertInstanceOf(CPResponse.class, value);
        CPResponse response = (CPResponse) value;
        assertEquals(100, response.getCode());
        assertEquals("invalid response args", response.getData().get("msg").asText());
    }
}

