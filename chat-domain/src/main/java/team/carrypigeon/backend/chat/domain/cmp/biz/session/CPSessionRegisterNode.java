package team.carrypigeon.backend.chat.domain.cmp.biz.session;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;

/**
 * 用于注册会话<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * 出参：SessionId:Long<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPSessionRegister")
public class CPSessionRegisterNode extends CPNodeComponent {

    private final CPSessionCenterService cpSessionCenterService;
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUser user = getBindData("UserInfo", CPUser.class);
        if (user == null){
            argsError(context);
        }
        // 注册会话
        cpSessionCenterService.addSession(user.getId(), session);
        // 将用户id注册进会话上下文
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());
    }
}
