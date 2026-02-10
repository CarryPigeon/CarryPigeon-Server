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

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 令牌实体在上下文中的键 { USER_TOKEN_INFO}
     */
    @Override
    protected CPKey<CPUserToken> getContextKey() {
        return CPNodeUserTokenKeys.USER_TOKEN_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPUserToken}
     */
    @Override
    protected Class<CPUserToken> getEntityClass() {
        return CPUserToken.class;
    }

    /**
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的令牌实体
     * @return {@code true} 表示令牌写库成功
     */
    @Override
    protected boolean doSave(CPUserToken entity) {
        return userTokenDao.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save user token error";
    }
}
