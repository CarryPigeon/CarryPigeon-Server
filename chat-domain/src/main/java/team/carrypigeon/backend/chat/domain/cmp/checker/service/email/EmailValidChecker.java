package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 邮箱验证组件<br/>
 * 验证邮箱是否合法<br/>
 * 入参：Email:String<br/>
 * 出参：失败则在上下文中加入参数错误的response<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("EmailValidChecker")
public class EmailValidChecker extends CPNodeComponent {
    private final static String EMAIL_VALID_CHECKER_PARAM = "Email";
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 判断参数是否存在
        String email = context.getData(EMAIL_VALID_CHECKER_PARAM);
        if (email == null||!email.matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("email error"));
            throw new CPReturnException();
        }
    }
}