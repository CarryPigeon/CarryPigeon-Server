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

/**
 * 用于保存用户信息的Node<br/>
 * 入参：UserInfo:CPUser<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserSaver")
public class CPUserSaverNode extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUser userInfo = context.getData("UserInfo");
        if (!userDao.save(userInfo)) {
            context.setData("response",CPResponse.ERROR_RESPONSE.copy().setTextData("save user error"));
            throw new CPReturnException();
        }
    }
}
