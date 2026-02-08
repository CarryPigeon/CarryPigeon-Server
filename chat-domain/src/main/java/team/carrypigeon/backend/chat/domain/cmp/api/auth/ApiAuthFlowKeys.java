package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * Internal flow keys for {@code /api/auth/*} chains.
 * <p>
 * These keys are not part of the public API contract. They are used to pass intermediate results between nodes
 * (Biz → Result) to ensure only Result nodes write {@code CPFlowKeys.RESPONSE}.
 */
public final class ApiAuthFlowKeys {

    public static final CPKey<ApiTokenResponse> TOKEN_RESPONSE =
            CPKey.of("api_auth_token_response", ApiTokenResponse.class);

    private ApiAuthFlowKeys() {
    }
}

