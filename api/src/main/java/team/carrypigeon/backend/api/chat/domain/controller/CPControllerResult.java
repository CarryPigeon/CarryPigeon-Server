package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

/**
 * 所有controller的返回结果接口<br/>
 * @author midreamsheep
 * */
public interface CPControllerResult {
    void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper);

    default void argsError(CPFlowContext context){
        Logger log = LoggerFactory.getLogger(this.getClass());
        log.error("CPControllerResult argsError: missing or invalid response context data in {}", this.getClass().getSimpleName());
        context.setData(
                "response",
                CPResponse.error("invalid response args")
        );
    }
}
