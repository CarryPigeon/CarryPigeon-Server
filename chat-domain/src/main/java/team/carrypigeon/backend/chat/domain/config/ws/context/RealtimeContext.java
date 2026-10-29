package team.carrypigeon.backend.chat.domain.config.ws.context;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;

import java.time.Instant;

public class RealtimeContext {
    /** 会话 ID。 */
    private static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("cp.realtime.session-id");

    /** 已认证主体。 */
    private static final AttributeKey<CpPrincipal> AUTHENTICATED_PRINCIPAL_KEY =
        AttributeKey.valueOf("cp.realtime.authenticated-principal");

    /** 认证超时任务。 */
    private static final AttributeKey<ScheduledFuture<?>> AUTHENTICATION_TIMEOUT_FUTURE_KEY =
        AttributeKey.valueOf("cp.realtime.authentication-timeout-future");

    /** access token 过期时间。 */
    private static final AttributeKey<Instant> ACCESS_TOKEN_EXPIRES_AT_KEY =
        AttributeKey.valueOf("cp.realtime.access-token-expires-at");

    /** access token 过期任务。 */
    private static final AttributeKey<ScheduledFuture<?>> ACCESS_TOKEN_EXPIRATION_FUTURE_KEY =
        AttributeKey.valueOf("cp.realtime.access-token-expiration-future");

    public static void setSessionIdKey(Channel channel, String sessionId){
        channel.attr(SESSION_ID_KEY).set(sessionId);
    }

    public static String getSessionIdKey(Channel channel){
        return channel.attr(SESSION_ID_KEY).get();
    }


    public static void setAuthenticatedPrincipalKey(Channel channel, CpPrincipal authenticatedPrincipal){
        channel.attr(AUTHENTICATED_PRINCIPAL_KEY).set(authenticatedPrincipal);
    }

    public static CpPrincipal getAuthenticatedPrincipalKey(Channel channel){
        return channel.attr(AUTHENTICATED_PRINCIPAL_KEY).get();
    }

    public static void setAccessTokenExpiresAtKey(Channel channel, Instant accessTokenExpiresAt){
        channel.attr(ACCESS_TOKEN_EXPIRES_AT_KEY).set(accessTokenExpiresAt);
    }

    public static Instant getAccessTokenExpiresAtKey(Channel channel){
        return channel.attr(ACCESS_TOKEN_EXPIRES_AT_KEY).get();
    }

    public static void setAuthenticationTimeoutFutureKey(Channel channel, ScheduledFuture<?> authenticationTimeoutFuture){
        channel.attr(AUTHENTICATION_TIMEOUT_FUTURE_KEY).set(authenticationTimeoutFuture);
    }

    public static ScheduledFuture<?> getAuthenticationTimeoutFutureKey(Channel channel){
        return channel.attr(AUTHENTICATION_TIMEOUT_FUTURE_KEY).get();
    }

    public static void setAccessTokenExpirationFutureKey(Channel channel, ScheduledFuture<?> accessTokenExpirationFuture){
        channel.attr(ACCESS_TOKEN_EXPIRATION_FUTURE_KEY).set(accessTokenExpirationFuture);
    }
    public static ScheduledFuture<?> getAccessTokenExpirationFutureKey(Channel channel){
        return channel.attr(ACCESS_TOKEN_EXPIRATION_FUTURE_KEY).get();
    }
}
