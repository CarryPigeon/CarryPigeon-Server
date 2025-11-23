package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 邮箱存在性校验组件<br/>
 * 验证邮箱是否存在<br/>
 * 入参：Email:String<br/>
 * 出参：失败则在上下文中加入参数错误的response<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("EmailExistsChecker")
public class EmailExistsChecker extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String email = context.getData("Email");
        if (email == null){
            argsError(context);
        }
        if (userDao.getByEmail(email) != null){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("email exists"));
            throw new CPReturnException();
        }
    }
}
