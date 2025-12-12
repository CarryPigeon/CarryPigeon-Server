package team.carrypigeon.backend.api.chat.domain.node;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * LiteFlow 普通节点基类。
 * <p>
 * 抽取从上下文中获取 {@link CPSession} 的通用逻辑，
 * 子类只需要实现 {@link #process(CPSession, DefaultContext)} 执行业务。
 * <p>
 * 同时对常见错误场景（必填参数缺失、业务失败等）统一打日志，便于排查。
 */
public abstract class CPNodeComponent extends NodeComponent {

    private static final Logger log = LoggerFactory.getLogger(CPNodeComponent.class);

    @Override
    public void process() throws Exception {
        // 获取 LiteFlow 默认上下文
        DefaultContext context = this.getContextBean(DefaultContext.class);
        // 从上下文中获取当前会话 CPSession
        CPSession session = context.getData(CPNodeCommonKeys.SESSION);
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
     * 从上下文中读取必填数据，如果为 null 则视为参数错误并中断流程。
     */
    protected <T> T requireContext(DefaultContext context, String key, Class<T> type) throws CPReturnException {
        T value = context.getData(key);
        if (value == null) {
            log.error("requireContext missing key '{}' (type={}) in node {}",
                    key, type == null ? null : type.getSimpleName(), getNodeId());
            argsError(context);
        }
        return value;
    }

    /**
     * 从绑定参数中读取必填配置，如果为 null 则视为参数错误并中断流程。
     */
    protected <T> T requireBind(DefaultContext context, String key, Class<T> type) throws CPReturnException {
        T value = getBindData(key, type);
        if (value == null) {
            log.error("requireBind missing bind key '{}' (type={}) in node {}",
                    key, type == null ? null : type.getSimpleName(), getNodeId());
            argsError(context);
        }
        return value;
    }

    /**
     * 业务失败时的统一处理：写入错误响应并中断流程。
     */
    protected void businessError(DefaultContext context, String message) throws CPReturnException {
        log.error("businessError in node {}: {}", getNodeId(), message);
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.ERROR_RESPONSE.copy().setTextData(message)
        );
        throw new CPReturnException();
    }

    /**
     * 参数异常时的统一处理：写入错误响应并中断流程。
     *
     * @param context LiteFlow 默认上下文
     * @throws CPReturnException 用于提前终止 LiteFlow 流程
     */
    protected void argsError(DefaultContext context) throws CPReturnException {
        log.error("argsError in node {}: invalid or missing arguments", getNodeId());
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.ERROR_RESPONSE.copy().setTextData("error args")
        );
        throw new CPReturnException();
    }
}
