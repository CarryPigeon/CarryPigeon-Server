package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

public class CPControllerDefaultResult implements CPControllerResult {
    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        // 空处理
    }
}
