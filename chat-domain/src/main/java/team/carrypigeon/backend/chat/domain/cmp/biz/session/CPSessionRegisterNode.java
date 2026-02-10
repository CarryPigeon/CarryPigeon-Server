package team.carrypigeon.backend.chat.domain.cmp.biz.session;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;

/**
 * 用于注册会话<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * 出参：session_uid:Long<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPSessionRegister")
public class CPSessionRegisterNode extends CPNodeComponent {

    private final CPSessionCenterService cpSessionCenterService;
    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param session 当前连接会话，写入用户标识供后续链路使用
     * @param context LiteFlow 上下文，提供会话 UID 数据源
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPUser user = requireContext(context, CPNodeUserKeys.USER_INFO);
        cpSessionCenterService.addSession(user.getId(), session);
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());
    }
}
