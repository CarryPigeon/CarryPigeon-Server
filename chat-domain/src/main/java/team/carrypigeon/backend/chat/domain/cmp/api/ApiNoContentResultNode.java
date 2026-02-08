package team.carrypigeon.backend.chat.domain.cmp.api;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;

/**
 * Result node for endpoints that return no body (HTTP 204).
 * <p>
 * Controllers may still set their own HTTP status (e.g. via {@code ResponseEntity.noContent()}).
 * This node returns {@code null}, so {@code CPFlowKeys.RESPONSE} is not written.
 */
@Slf4j
@LiteflowComponent("ApiNoContentResult")
public class ApiNoContentResultNode extends AbstractResultNode<Void> {
    @Override
    protected Void build(CPFlowContext context) {
        log.debug("ApiNoContentResult success");
        return null;
    }
}
