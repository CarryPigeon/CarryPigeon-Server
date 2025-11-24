package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

/**
 * 所有controller的返回结果接口<br/>
 * @author midreamsheep
 * */
public interface CPControllerResult {
    void process(CPSession session, DefaultContext context, ObjectMapper objectMapper);

    default void argsError(DefaultContext context){
        context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("error args"));
    }
}