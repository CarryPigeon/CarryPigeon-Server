package team.carrypigeon.backend.chat.domain.controller.web.api.flow;

import com.yomahub.liteflow.flow.LiteflowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.flow.FlowTxExecutor;

/**
 * HTTP API 的 LiteFlow 执行入口。
 * <p>
 * 负责在事务上下文中执行指定 chain，并将未知异常统一折叠为标准业务异常。
 */
@Service
public class ApiFlowRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiFlowRunner.class);

    private final FlowTxExecutor flowTxExecutor;

    /**
     * 构造 API 链路执行器。
     *
     * @param flowTxExecutor 事务化 LiteFlow 执行器。
     */
    public ApiFlowRunner(FlowTxExecutor flowTxExecutor) {
        this.flowTxExecutor = flowTxExecutor;
    }

    /**
     * 执行指定 LiteFlow 链路。
     *
     * @param chain 链路名称。
     * @param context 链路上下文。
     * @return 执行后的上下文。
     * @throws CPProblemException 当链路执行失败时抛出。
     */
    public CPFlowContext executeOrThrow(String chain, CPFlowContext context) {
        log.debug("[ApiFlowRunner] 开始执行链路: chain={}", chain);

        LiteflowResponse resp = flowTxExecutor.executeWithTx(chain, context);

        if (!resp.isSuccess()) {
            Exception cause = resp.getCause();
            log.warn("[ApiFlowRunner] 链路执行失败: chain={}, error={}",
                    chain, cause != null ? cause.getMessage() : "unknown");

            if (cause instanceof CPProblemException pex) {
                throw pex;
            }
            throw new CPProblemException(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "internal error"));
        }

        log.debug("[ApiFlowRunner] 链路执行成功: chain={}", chain);
        return context;
    }
}
