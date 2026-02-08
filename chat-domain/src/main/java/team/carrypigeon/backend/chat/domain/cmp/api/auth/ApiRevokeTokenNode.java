package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.RefreshTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RevokeRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * Revoke (delete) a refresh token.
 * <p>
 * Route: {@code POST /api/auth/revoke} (public)
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link RevokeRequest}
 * Output: none (HTTP 204 is handled by controller)
 * <p>
 * This endpoint is idempotent: revoking an unknown token is treated as success.
 */
@Slf4j
@LiteflowComponent("ApiRevokeToken")
@RequiredArgsConstructor
public class ApiRevokeTokenNode extends CPNodeComponent {

    private final RefreshTokenService refreshTokenService;

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof RevokeRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        CPUserToken token = refreshTokenService.getByToken(req.refreshToken());
        if (token != null) {
            refreshTokenService.revoke(token);
        }
        log.debug("ApiRevokeToken success");
    }
}
