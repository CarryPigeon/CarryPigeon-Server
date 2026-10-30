package team.carrypigeon.backend.chat.domain.config.ws.transport;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;

/**
 * 实时连接认证状态机。
 * 职责：承接凭证校验、principal 绑定、认证超时与 token 过期定时、断连注销。
 */
@Slf4j
@Component
public final class NettyRealtimeAuthState implements RealtimeAuthState {

    private final NettyRealtimeConnectionManager connectionManager;
    private final AuthTokenApi authTokenApi;

    public NettyRealtimeAuthState(
        NettyRealtimeConnectionManager cpNettyRealtimeConnectionManager,
        AuthTokenApi authTokenApi
    ) {
        this.authTokenApi = authTokenApi;
        this.connectionManager = cpNettyRealtimeConnectionManager;
    }

    @Override
    public RealtimeAuthSession authenticate(
        RealtimeConnection connection,
        RealtimeAttributes attributes,
        RealtimeScheduler scheduler,
        String accessToken
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw ProblemException.of(AuthProblem.AUTHENTICATION_REQUIRED);
        }

        AccessTokenAuthenticationResult result = authTokenApi.authenticate(accessToken);

        // 重写认证信息
        Long previousAccountId = attributes.getAccountId();
        if (previousAccountId != null && !Objects.equals(previousAccountId, result.accountId())) {
            connectionManager.unregister(previousAccountId, connection);
        }
        attributes.setAccountId(result.accountId());
        connectionManager.register(result.accountId(), connection);

        scheduler.scheduleAccessTokenExpiration(result.expiresAt());

        return new RealtimeAuthSession(result.accountId(), result.expiresAt());
    }

    @Override
    public void onChannelInactive(
        RealtimeConnection connection,
        RealtimeAttributes attributes,
        RealtimeScheduler scheduler
    ) {
        scheduler.cancelAuthTimeout();
        scheduler.cancelTokenExpiration();
        Long accountId = attributes.getAccountId();
        if (accountId != null) {
            connectionManager.unregister(accountId, connection);
        }
    }
}
