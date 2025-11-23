package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 邮箱验证码检查器</br>
 * 验证邮箱验证码是否正确<br/>
 * 入参：Email:String;Email_Code:Long<br/>
 * 出参：不一致则将错误response加入context<br/>
 * */
@AllArgsConstructor
@LiteflowComponent("EmailCodeChecker")
public class EmailCodeChecker extends CPNodeComponent {

    private final CPCache cache;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 获取参数
        String email = context.getData("Email");
        Long code = context.getData("Email_Code");
        if (email == null || code == null){
            context.setData("response", CPResponse.SERVER_ERROR.copy().setTextData("email code param error"));
            throw new CPReturnException();
        }
        String serverCode = cache.getAndDelete(email + ":code");
        if (serverCode == null || !serverCode.equals(code.longValue() + "")){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("email code error"));
            throw new CPReturnException();
        }
        // 成功执行则不错处理
    }
}