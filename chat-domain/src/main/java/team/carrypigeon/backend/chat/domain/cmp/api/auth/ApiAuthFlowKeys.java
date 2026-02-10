package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * `/api/auth/*` 链路内部上下文键。
 * <p>
 * 这些键属于链路内部协议，不直接暴露给外部 API。
 */
public final class ApiAuthFlowKeys {

    /**
     * 令牌响应对象（由业务节点写入，结果节点读取）。
     */
    public static final CPKey<ApiTokenResponse> TOKEN_RESPONSE =
            CPKey.of("api_auth_token_response", ApiTokenResponse.class);

    /**
     * 工具类禁止实例化。
     */
    private ApiAuthFlowKeys() {
    }
}
