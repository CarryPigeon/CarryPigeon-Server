package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * Access token 认证领域 API 实现。
 * 职责：调用 auth 内部 token codec，并把内部 claims 收敛为稳定认证投影。
 * 边界：不签发 token，不处理 refresh session。
 */
@Service
public class AccessTokenAuthenticationDomainApi implements AccessTokenAuthenticationApi {

    private final AuthTokenCodec authTokenCodec;

    public AccessTokenAuthenticationDomainApi(AuthTokenCodec authTokenCodec) {
        this.authTokenCodec = authTokenCodec;
    }

    @Override
    public AccessTokenAuthenticationResult authenticate(String accessToken) {
        AuthTokenClaims claims = authTokenCodec.parseAccessToken(accessToken);
        return new AccessTokenAuthenticationResult(
                parseAccountId(claims.subject()),
                claims.username(),
                claims.expiresAt()
        );
    }

    private long parseAccountId(String subject) {
        try {
            long accountId = Long.parseLong(subject);
            if (accountId <= 0L) {
                throw invalidAccessToken();
            }
            return accountId;
        } catch (NumberFormatException exception) {
            throw invalidAccessToken();
        }
    }

    private ProblemException invalidAccessToken() {
        return ProblemException.forbidden("invalid_access_token", "access token is invalid");
    }
}
