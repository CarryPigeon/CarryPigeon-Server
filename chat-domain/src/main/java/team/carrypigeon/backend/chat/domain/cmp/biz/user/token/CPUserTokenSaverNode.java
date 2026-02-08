package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 保存用户token的Node<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenSaver")
public class CPUserTokenSaverNode extends AbstractSaveNode<CPUserToken> {

    private final UserTokenDao userTokenDao;

    @Override
    protected CPKey<CPUserToken> getContextKey() {
        return CPNodeUserTokenKeys.USER_TOKEN_INFO;
    }

    @Override
    protected Class<CPUserToken> getEntityClass() {
        return CPUserToken.class;
    }

    @Override
    protected boolean doSave(CPUserToken entity) {
        return userTokenDao.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save user token error";
    }
}
