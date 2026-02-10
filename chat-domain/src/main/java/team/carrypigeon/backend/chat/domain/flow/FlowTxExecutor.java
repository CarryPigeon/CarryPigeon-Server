package team.carrypigeon.backend.chat.domain.flow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * LiteFlow 事务执行器。
 * <p>
 * 将单条 chain 执行包裹在同一事务中，链路失败时统一标记回滚。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlowTxExecutor {

    private final FlowExecutor flowExecutor;

    /**
     * 在事务内执行指定 LiteFlow chain。
     *
     * @param chain LiteFlow chain 名称。
     * @param context LiteFlow 上下文。
     * @return LiteFlow 执行结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public LiteflowResponse executeWithTx(String chain, CPFlowContext context) {
        LiteflowResponse response = flowExecutor.execute2Resp(chain, null, context);
        if (!response.isSuccess()) {
            log.debug("LiteFlow chain {} execute failed, mark transaction rollback. message={}",
                    chain, response.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return response;
    }
}
