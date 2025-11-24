package team.carrypigeon.backend.api.chat.domain.controller;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        // 获取默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文获取CPSession
        CPSession session = context.getData("session");
        // 调用抽象方法
        return process(session,context);
    }

    protected abstract String process(CPSession session,DefaultContext context) throws Exception;

    protected void argsError(DefaultContext context) throws CPReturnException{
        context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("error args"));
        throw new CPReturnException();
    }
}