package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.RefreshTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RefreshRequest;

/**
 * 刷新令牌节点。
 * <p>
 * 路由：`POST /api/auth/refresh`（公开接口）。
 * <p>
 * 刷新策略：
 * - refresh token 单次使用，刷新时会吊销旧 token；
 * - 过期 token 会被清理并返回 `token_expired`。
 */
@Slf4j
@LiteflowComponent("ApiRefreshTokens")
@RequiredArgsConstructor
public class ApiRefreshTokensNode extends CPNodeComponent {

    private final CpApiProperties properties;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 执行 refresh token 换发流程。
     *
     * @param context LiteFlow 上下文，需包含 {@link CPFlowKeys#REQUEST}
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof RefreshRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        CPUserToken old = refreshTokenService.getByToken(req.refreshToken());
        if (old == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "invalid refresh token"));
        }
        if (refreshTokenService.isExpired(old)) {
            refreshTokenService.revoke(old);
            throw new CPProblemException(CPProblem.of(CPProblemReason.TOKEN_EXPIRED, "refresh token expired"));
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
