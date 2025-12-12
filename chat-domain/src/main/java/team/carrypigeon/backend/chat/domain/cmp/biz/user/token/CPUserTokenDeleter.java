package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 删除用户token<br/>
 * 入参：UserToken:{@link CPUserToken}<br/>
 * 出参：无
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenDeleter")
public class CPUserTokenDeleter extends AbstractDeleteNode<CPUserToken> {

    private final UserTokenDao userTokenDao;

    @Override
    protected String getContextKey() {
        return CPNodeUserTokenKeys.USER_TOKEN_INFO;
    }

    @Override
    protected Class<CPUserToken> getEntityClass() {
        return CPUserToken.class;
    }

    @Override
    protected boolean doDelete(CPUserToken entity) {
        return userTokenDao.delete(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "error deleting token";
    }
}
