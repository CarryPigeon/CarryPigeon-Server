package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;

/**
 * 幂等键处于处理中时的快速失败节点。
 */
@LiteflowComponent("ApiMessageIdempotencyPendingFail")
public class ApiMessageIdempotencyPendingFailNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        fail(CPProblem.of(409, "idempotency_processing", "request with same idempotency key is processing"));
    }
}

