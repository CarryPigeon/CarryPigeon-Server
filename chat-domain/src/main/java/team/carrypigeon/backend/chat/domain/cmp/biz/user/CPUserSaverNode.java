package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 用于持久化保存用户信息的节点。<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * 出参：无（保存失败时抛出 {@link team.carrypigeon.backend.api.chat.domain.error.CPProblemException}）<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSaver")
public class CPUserSaverNode extends AbstractSaveNode<CPUser> {

    private final UserDao userDao;

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 用户实体在上下文中的键 { USER_INFO}
     */
    @Override
    protected CPKey<CPUser> getContextKey() {
        return CPNodeUserKeys.USER_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPUser}
     */
    @Override
    protected Class<CPUser> getEntityClass() {
        return CPUser.class;
    }

    /**
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的用户实体
     * @return {@code true} 表示用户写库成功
     */
    @Override
    protected boolean doSave(CPUser entity) {
        return userDao.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save user error";
    }
}
