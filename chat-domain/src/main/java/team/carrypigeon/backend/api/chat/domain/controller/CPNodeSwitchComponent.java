package team.carrypigeon.backend.api.chat.domain.controller;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * LiteFlow 分支节点基类。
 * <p>
 * 封装了从上下文中获取 {@link CPSession} 的逻辑，
 * 子类只需要实现 {@link #process(CPSession, DefaultContext)} 返回分支标记。
 */
public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        // 获取 LiteFlow 默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文中获取当前会话 CPSession
        CPSession session = context.getData(CPNodeValueKeyBasicConstants.SESSION);
        // 委托子类执行业务并返回分支标签
        return process(session, context);
    }

    /**
     * 具体分支节点的业务逻辑。
     *
     * @param session 当前连接的会话信息
     * @param context LiteFlow 默认上下文
     * @return LiteFlow 分支标记
     */
    protected abstract String process(CPSession session, DefaultContext context) throws Exception;

    /**
     * 参数异常时的统一处理：写入错误响应并中断流程。
     *
     * @param context LiteFlow 默认上下文
     * @throws CPReturnException 用于提前终止 LiteFlow 流程
     */
    protected void argsError(DefaultContext context) throws CPReturnException {
        context.setData(
                CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.ERROR_RESPONSE.copy().setTextData("error args")
        );
        throw new CPReturnException();
    }
}