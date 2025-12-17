package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * LiteFlow 分支节点基类。
 * <p>
 * 封装了从上下文中获取 {@link CPSession} 的逻辑，
 * 子类只需要实现 {@link #process(CPSession, CPFlowContext)} 返回分支标记。
 */
public abstract class CPNodeSwitchComponent extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeSwitchComponent.class);

    @Override
    public String processSwitch() throws Exception {
        CPFlowContext context = this.getContextBean(CPFlowContext.class);
        if (context == null) {
            throw new IllegalStateException("CPFlowContext not found in LiteFlow slot, node=" + getNodeId());
        }
        // 从上下文中获取当前会话 CPSession
        CPSession session = context.getData(CPNodeCommonKeys.SESSION);
        // 委托子类执行业务并返回分支标签
        return process(session, context);
    }

    /**
     * 具体分支节点的业务逻辑。
     *
     * @param session 当前连接的会话信息
     * @param context LiteFlow 上下文
     * @return LiteFlow 分支标记
     */
    protected abstract String process(CPSession session, CPFlowContext context) throws Exception;

    /**
     * 参数异常时的统一处理：写入错误响应并中断流程。
     *
     * @param context LiteFlow 上下文
     * @throws CPReturnException 用于提前终止 LiteFlow 流程
     */
    protected void argsError(CPFlowContext context) throws CPReturnException {
        log.error("CPNodeSwitchComponent argsError in node {}: invalid or missing arguments", getNodeId());
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.error("error args")
        );
        throw new CPReturnException();
    }
}
