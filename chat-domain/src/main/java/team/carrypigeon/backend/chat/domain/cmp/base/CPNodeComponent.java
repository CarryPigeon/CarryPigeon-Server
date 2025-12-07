package team.carrypigeon.backend.chat.domain.cmp.base;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

public abstract class CPNodeComponent extends NodeComponent {

    @Override
    public void process() throws Exception {
        // 获取默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文获取CPSession
        CPSession session = context.getData("session");
        // 调用抽象方法
        process(session,context);
    }

    public abstract void process(CPSession session, DefaultContext context) throws Exception;

    protected void argsError(DefaultContext context) throws CPReturnException{
        context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("error args"));
        throw new CPReturnException();
    }
}
