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
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.RefreshTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RevokeRequest;

/**
 * 吊销刷新令牌节点。
 * <p>
 * 路由：`POST /api/auth/revoke`（公开接口）。
 * <p>
 * 幂等语义：即使 token 不存在也视为成功。
 */
@Slf4j
@LiteflowComponent("ApiRevokeToken")
@RequiredArgsConstructor
public class ApiRevokeTokenNode extends CPNodeComponent {

    private final RefreshTokenService refreshTokenService;

    /**
     * 执行刷新令牌吊销。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof RevokeRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        CPUserToken token = refreshTokenService.getByToken(req.refreshToken());
        if (token != null) {
            refreshTokenService.revoke(token);
        }
        log.debug("ApiRevokeToken success");
    }
}
