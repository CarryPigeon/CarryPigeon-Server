package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

import java.util.Objects;

/**
 * 用户注册响应node,用于对用户进行响应<br/>
 * 入参为UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
public class CPUserRegisterResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPUserToken userToken = context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO);
        if (userToken == null){
            argsError(context);
            return;
        }
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy()
                        .setData(JsonNodeUtil.createJsonNode("token",
                                Objects.requireNonNull(userToken).getToken())));
    }
}
