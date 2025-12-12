package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 用于持久化保存用户信息的节点。<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * 出参：无（保存失败时会设置错误响应并抛出 {@link CPReturnException}）<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSaver")
public class CPUserSaverNode extends AbstractSaveNode<CPUser> {

    private final UserDao userDao;

    @Override
    protected String getContextKey() {
        return CPNodeUserKeys.USER_INFO;
    }

    @Override
    protected Class<CPUser> getEntityClass() {
        return CPUser.class;
    }

    @Override
    protected boolean doSave(CPUser entity) {
        return userDao.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save user error";
    }
}
