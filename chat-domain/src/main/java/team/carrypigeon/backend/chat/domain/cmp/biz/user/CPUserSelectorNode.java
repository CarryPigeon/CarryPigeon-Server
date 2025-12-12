package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 根据用户 id 或邮箱从数据库中查询用户信息的 Selector 节点。<br/>
 * 查询模式通过 bind 参数 key 指定："id" / "email"。<br/>
 * 1. id 查询入参：UserInfo_Id:Long<br/>
 * 2. email 查询入参：UserInfo_Email:String<br/>
 * 出参：UserInfo:{@link CPUser}<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSelector")
public class CPUserSelectorNode extends AbstractSelectorNode<CPUser> {

    private final UserDao userDao;

    @Override
    protected CPUser doSelect(String mode, DefaultContext context) throws Exception {
        switch (mode) {
            case "id":
                Long id = requireContext(context, CPNodeUserKeys.USER_INFO_ID, Long.class);
                return userDao.getById(id);
            case "email":
                String email = requireContext(context, CPNodeUserKeys.USER_INFO_EMAIL, String.class);
                return userDao.getByEmail(email);
            default:
                argsError(context);
                return null;
        }
    }

    @Override
    protected String getResultKey() {
        return CPNodeUserKeys.USER_INFO;
    }

    @Override
    protected void handleNotFound(String mode, DefaultContext context) throws CPReturnException {
        businessError(context, "user not found");
    }
}
