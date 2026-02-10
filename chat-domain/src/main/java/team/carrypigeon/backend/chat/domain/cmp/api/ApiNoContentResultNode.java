package team.carrypigeon.backend.chat.domain.cmp.api;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;

/**
 * 无响应体结果节点。
 * <p>
 * 适用于返回 `HTTP 204` 的接口；该节点返回 `null`，不写入业务响应体。
 */
@Slf4j
@LiteflowComponent("ApiNoContentResult")
public class ApiNoContentResultNode extends AbstractResultNode<Void> {

    /**
     * 返回空结果。
     */
    @Override
    protected Void build(CPFlowContext context) {
        log.debug("ApiNoContentResult success");
        return null;
    }
}
