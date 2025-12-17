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
 * 负责在统一事务中执行 LiteFlow chain 的执行器。
 * <p>
 * 所有在同一条 chain 中通过 MyBatis-Plus DAO 执行的数据库写操作
 * 都会加入到同一个 Spring 事务中，任意节点执行失败时整体回滚。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlowTxExecutor {

    private final FlowExecutor flowExecutor;

    /**
     * 在事务中执行指定的 LiteFlow chain。
     *
     * @param chain   LiteFlow chain 名称（通常为路由 path）
     * @param context LiteFlow 上下文
     * @return LiteflowResponse 执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public LiteflowResponse executeWithTx(String chain, CPFlowContext context) {
        LiteflowResponse response = flowExecutor.execute2Resp(chain, null, context);
        if (!response.isSuccess()) {
            // LiteFlow 标记执行失败时，将当前事务标记为回滚
            log.debug("LiteFlow chain {} execute failed, mark transaction rollback. message={}",
                    chain, response.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return response;
    }
}
