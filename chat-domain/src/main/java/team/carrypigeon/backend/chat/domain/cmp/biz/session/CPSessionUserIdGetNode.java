package team.carrypigeon.backend.chat.domain.cmp.biz.session;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 用于获取当前会话中的用户信息<br/>
 * 入参：无<br/>
 * 出参: SessionId:Long<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPSessionUserIdGet")
public class CPSessionUserIdGetNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue==null){
            argsError(context);
        }
        context.setData("SessionId",attributeValue);
    }
}