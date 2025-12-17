package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
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
    protected CPUserToken doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode){
            case "token":
                String token =
                        requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, String.class);
                return select(context,
                        buildSelectKey("user_token", "token", token),
                        () -> userTokenDao.getByToken(token));
            case "id":
                Long id = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_ID, Long.class);
                return select(context,
                        buildSelectKey("user_token", "id", id),
                        () -> userTokenDao.getById(id));
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
    protected void handleNotFound(String mode, CPFlowContext context) throws CPReturnException {
        businessError(context, "token does not exists");
    }
}
