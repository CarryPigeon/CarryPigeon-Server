package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;

/**
 * Result node for token endpoints.
 * <p>
 * Input: {@link ApiAuthFlowKeys#TOKEN_RESPONSE} = {@link ApiTokenResponse}
 * Output: {@code CPFlowKeys.RESPONSE} = {@link ApiTokenResponse}
 */
@Slf4j
@LiteflowComponent("ApiTokensResult")
public class ApiTokensResultNode extends AbstractResultNode<ApiTokenResponse> {

    @Override
    protected ApiTokenResponse build(CPFlowContext context) {
        ApiTokenResponse response = requireContext(context, ApiAuthFlowKeys.TOKEN_RESPONSE);
        log.debug("ApiTokensResult success, uid={}", response.uid());
        return response;
    }
}

