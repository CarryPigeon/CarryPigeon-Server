package team.carrypigeon.backend.api.chat.domain.controller;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeSwitchComponent.class);

    @Override
    public String processSwitch() throws Exception {
        // 获取默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文获取 CPSession
        CPSession session = context.getData("session");
        // 调用抽象方法
        return process(session, context);
    }

    protected abstract String process(CPSession session, DefaultContext context) throws Exception;

    protected void argsError(DefaultContext context) throws CPReturnException {
        log.error("argsError in switch node {}: invalid or missing arguments", getNodeId());
        context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("error args"));
        throw new CPReturnException();
    }
}
