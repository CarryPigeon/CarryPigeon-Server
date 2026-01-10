package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

import static org.junit.jupiter.api.Assertions.*;

class CPControllerDefaultResultTests {

    @Test
    void process_shouldDoNothing() {
        CPFlowContext context = new CPFlowContext();
        assertDoesNotThrow(() -> new CPControllerDefaultResult().process(null, context, new ObjectMapper()));
    }
}

