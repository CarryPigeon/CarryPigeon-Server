package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 用户通过token登录的结果<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenLoginResult")
public class CPUserTokenLoginResultNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData("UserToken");
        if (userToken == null){
            argsError(context);
        }
        Result result = new Result(userToken.getToken(), userToken.getUid());
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Result{
        private String token;
        private long uid;
    }
}
