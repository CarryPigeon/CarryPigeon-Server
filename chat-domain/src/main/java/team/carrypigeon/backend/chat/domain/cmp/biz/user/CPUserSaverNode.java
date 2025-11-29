package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 用于持久化保存用户信息的节点。<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * 出参：无（保存失败时会设置错误响应并抛出 {@link CPReturnException}）<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSaver")
public class CPUserSaverNode extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPUser userInfo = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);
        if (!userDao.save(userInfo)) {
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("save user error"));
            throw new CPReturnException();
        }
    }
}
