package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 用于从数据库查询token<br/>
 * bind绑定查询格式key:{token|id}<br/>
 * 入参: UserToken_Token:{@link String}|id:{@link Long}<br/>
 * 出参: UserToken:{@link CPUserToken}
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenSelector")
public class CPUserTokenSelectorNode extends AbstractSelectorNode<CPUserToken> {
    private final UserTokenDao userTokenDao;

    @Override
    protected CPUserToken doSelect(String mode, DefaultContext context) throws Exception {
        switch (mode){
            case "token":
                String token =
                        requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, String.class);
                return userTokenDao.getByToken(token);
            case "id":
                Long id = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_ID, Long.class);
                return userTokenDao.getById(id);
            default:
                argsError(context);
                return null;
        }
    }

    @Override
    protected String getResultKey() {
        return CPNodeUserTokenKeys.USER_TOKEN_INFO;
    }

    @Override
    protected void handleNotFound(String mode, DefaultContext context) throws CPReturnException {
        businessError(context, "token does not exists");
    }
}
