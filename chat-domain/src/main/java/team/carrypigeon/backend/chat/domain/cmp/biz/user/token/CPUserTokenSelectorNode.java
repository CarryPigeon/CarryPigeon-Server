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
 * 用于从数据库查询token<br/>
 * bind绑定查询格式key:{token|id}<br/>
 * 入参: UserToken_Token:{@link String}|id:{@link Long}<br/>
 * 出参: UserToken:{@link CPUserToken}
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenSelector")
public class CPUserTokenSelectorNode extends CPNodeComponent {
    private final UserTokenDao userTokenDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        if (bindData == null){
            argsError(context);
        }
        switch (bindData){
            case "token":
                CPUserToken userToken = userTokenDao.getByToken(context.getData("UserToken_Token"));
                if (userToken == null){
                    context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("token does not exists"));
                    throw new CPReturnException();
                }
                context.setData("UserToken", userToken);
                break;
            case "id":
                CPUserToken userTokenId = userTokenDao.getById(context.getData("UserToken_Id"));
                if (userTokenId == null){
                    context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("token does not exists"));
                    throw new CPReturnException();
                }
                context.setData("UserToken", userTokenId);
                break;
            case null:
                break;
            default:
                break;
        }
    }
}
