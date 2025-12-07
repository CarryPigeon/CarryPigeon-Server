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
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

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
    public void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        if (bindData == null){
            argsError(context);
        }
        switch (bindData){
            case "token":
                CPUserToken userToken = userTokenDao.getByToken(
                        context.getData(CPNodeValueKeyBasicConstants.USER_TOKEN_INFO_TOKEN));
                if (userToken == null){
                    context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                            CPResponse.ERROR_RESPONSE.copy().setTextData("token does not exists"));
                    throw new CPReturnException();
                }
                context.setData(CPNodeValueKeyBasicConstants.USER_TOKEN_INFO, userToken);
                break;
            case "id":
                CPUserToken userTokenId = userTokenDao.getById(
                        context.getData(CPNodeValueKeyBasicConstants.USER_TOKEN_INFO_ID));
                if (userTokenId == null){
                    context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                            CPResponse.ERROR_RESPONSE.copy().setTextData("token does not exists"));
                    throw new CPReturnException();
                }
                context.setData(CPNodeValueKeyBasicConstants.USER_TOKEN_INFO, userTokenId);
                break;
            case null:
                break;
            default:
                break;
        }
    }
}
