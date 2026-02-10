package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;

/**
 * 令牌结果节点。
 * <p>
 * 从上下文读取 {@link ApiAuthFlowKeys#TOKEN_RESPONSE} 并作为 API 响应输出。
 */
@Slf4j
@LiteflowComponent("ApiTokensResult")
public class ApiTokensResultNode extends AbstractResultNode<ApiTokenResponse> {

    /**
     * 构建令牌响应。
     */
    @Override
    protected ApiTokenResponse build(CPFlowContext context) {
        ApiTokenResponse response = requireContext(context, ApiAuthFlowKeys.TOKEN_RESPONSE);
        log.debug("ApiTokensResult success, uid={}", response.uid());
        return response;
    }
}
