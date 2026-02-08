package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.RefreshTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RefreshRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * Refresh tokens using a refresh token.
 * <p>
 * Route: {@code POST /api/auth/refresh} (public)
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link RefreshRequest}
 * Output: {@link ApiAuthFlowKeys#TOKEN_RESPONSE} = {@link ApiTokenResponse}
 * <p>
 * Rotation strategy:
 * <ul>
 *   <li>Refresh token is single-use: on refresh, old token is revoked and a new token is issued.</li>
 *   <li>If token is expired, it is deleted and request fails.</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("ApiRefreshTokens")
@RequiredArgsConstructor
public class ApiRefreshTokensNode extends CPNodeComponent {

    private final CpApiProperties properties;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof RefreshRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        CPUserToken old = refreshTokenService.getByToken(req.refreshToken());
        if (old == null) {
            throw new CPProblemException(CPProblem.of(401, "unauthorized", "invalid refresh token"));
        }
        if (refreshTokenService.isExpired(old)) {
            refreshTokenService.revoke(old);
            throw new CPProblemException(CPProblem.of(401, "token_expired", "refresh token expired"));
        }

        refreshTokenService.revoke(old);
        int expiresIn = properties.getAuth().getAccessTokenTtlSeconds();
        String accessToken = accessTokenService.issue(old.getUid(), expiresIn);
        CPUserToken refreshToken = refreshTokenService.issue(old.getUid(), properties.getAuth().getRefreshTokenTtlDays());
        context.set(ApiAuthFlowKeys.TOKEN_RESPONSE,
                ApiTokenResponse.from(old.getUid(), accessToken, expiresIn, refreshToken.getToken(), false));
        log.debug("ApiRefreshTokens success, uid={}", old.getUid());
    }
}
