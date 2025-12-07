package team.carrypigeon.backend.api.chat.domain.controller;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * LiteFlow 普通节点基类。
 * <p>
 * 抽取从上下文中获取 {@link CPSession} 的通用逻辑，
 * 子类只需要实现 {@link #process(CPSession, DefaultContext)} 执行业务。
 */
public abstract class CPNodeComponent extends NodeComponent {

    @Override
    public void process() throws Exception {
        // 获取 LiteFlow 默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文中获取当前会话 CPSession
        CPSession session = context.getData(CPNodeValueKeyBasicConstants.SESSION);
        // 委托子类执行业务逻辑
        process(session, context);
    }

    /**
     * 具体节点的执行业务逻辑。
     *
     * @param session 当前连接的会话信息
     * @param context LiteFlow 默认上下文
     */
    public abstract void process(CPSession session, DefaultContext context) throws Exception;

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