package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCPResultTests {

    @Test
    void writeHelpers_shouldPopulateResponseInContext() {
        ObjectMapper mapper = new ObjectMapper();
        CPSession session = new TestSession();
        CPFlowContext context = new CPFlowContext();

        AbstractCPResult result = new AbstractCPResult() {
            @Override
            public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
                writeSuccess(context, objectMapper, null);
                writeSuccessField(context, objectMapper, "k", "v");
                writeError(context, "err");
            }
        };

        result.process(session, context, mapper);
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("err", response.getData().get("msg").asText());
    }
}

