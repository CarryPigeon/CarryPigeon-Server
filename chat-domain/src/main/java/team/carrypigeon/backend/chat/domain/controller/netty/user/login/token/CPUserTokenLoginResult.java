package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 用户通过token登录的结果<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
public class CPUserTokenLoginResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper)  {
        CPUserToken userToken = context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO);
        if (userToken == null){
            argsError(context);
            return;
        }
        Result result = new Result(userToken.getToken(), userToken.getUid());
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Result{
        private String token;
        private long uid;
    }
}
