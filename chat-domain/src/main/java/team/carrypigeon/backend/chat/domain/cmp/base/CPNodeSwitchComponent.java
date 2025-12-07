package team.carrypigeon.backend.chat.domain.cmp.base;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        DefaultContext context = this.getContextBean(DefaultContext.class);
        CPSession session = context.getData(CPNodeValueKeyBasicConstants.SESSION);
        return process(session, context);
    }

    protected abstract String process(CPSession session, DefaultContext context) throws Exception;

    protected void argsError(DefaultContext context) throws CPReturnException {
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.ERROR_RESPONSE.copy().setTextData("error args"));
        throw new CPReturnException();
    }
}
