package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 用户邮箱登录响应数据<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * */
public class CPUserEmailLoginResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPUserToken userToken = context.getData(CPNodeValueKeyExtraConstants.USER_TOKEN);
        if (userToken == null){
            argsError(context);
            return;
        }
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy()
                        .setData(objectMapper.valueToTree(new Result(userToken.getToken()))));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private String token;
    }
}
