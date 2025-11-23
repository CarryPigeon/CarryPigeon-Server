package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 用户邮箱登录响应数据<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserEmailLoginResult")
public class CPUserEmailLoginResultNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData("UserToken");
        if (userToken == null){
            argsError(context);
        }
        assert userToken != null;
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new Result(userToken.getToken()))));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private String token;
    }
}