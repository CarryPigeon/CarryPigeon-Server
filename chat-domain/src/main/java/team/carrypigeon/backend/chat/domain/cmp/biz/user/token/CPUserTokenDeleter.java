package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 删除用户token<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * 出参：无
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenDeleter")
public class CPUserTokenDeleter extends CPNodeComponent {

    private final UserTokenDao userTokenDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData("UserToken");
        if (userToken == null){
            argsError(context);
        }
        if (!userTokenDao.delete(userToken.getToken())){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting token"));
            throw new CPReturnException();
        }
    }
}
