package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 判断用户是否登录的校验器<br/>
 * 入参：无<br/>
 * 出参：SessionId:Long<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("UserLoginChecker")
public class UserLoginCheckerNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue==null){
            context.setData("response",CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user not login"));
            throw new CPReturnException();
        }
        context.setData("SessionId", attributeValue);
    }
}