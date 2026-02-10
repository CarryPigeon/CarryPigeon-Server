package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;

/**
 * 幂等键处于处理中时的快速失败节点。
 */
@LiteflowComponent("ApiMessageIdempotencyPendingFail")
public class ApiMessageIdempotencyPendingFailNode extends CPNodeComponent {

    /**
     * 直接返回幂等处理中错误。
     */
    @Override
    protected void process(CPFlowContext context) {
        fail(CPProblem.of(CPProblemReason.IDEMPOTENCY_PROCESSING, "request with same idempotency key is processing"));
    }
}
