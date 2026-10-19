package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时通道认证机制。
 * 职责：承接凭证校验、principal 绑定、认证超时与 token 过期定时、断连注销。
 * 边界：不解析上行帧、不承载续传逻辑；只有本类会主动写出认证类下行帧。
 */
final class RealtimeAuthenticationCoordinator {

    private final TimeProviderImpl timeProvider;
    private final AccessTokenAuthenticationApi accessTokenAuthenticationApi;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeConnectionIndex realtimeConnectionIndex;
    private final int authenticationTimeoutSeconds;
    private final RealtimeWebSocketDebugLogger debugLogger;
    private final RealtimeFrameCodec frameCodec;

    RealtimeAuthenticationCoordinator(
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeConnectionIndex realtimeConnectionIndex,
            int authenticationTimeoutSeconds,
            RealtimeWebSocketDebugLogger debugLogger,
            RealtimeFrameCodec frameCodec
    ) {
        this.timeProvider = timeProvider;
        this.accessTokenAuthenticationApi = accessTokenAuthenticationApi;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeConnectionIndex = realtimeConnectionIndex;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
        this.debugLogger = debugLogger;
        this.frameCodec = frameCodec;
    }

    /**
     * 校验凭证并绑定当前连接。
     * 失败语义：凭证缺失、无效或过期时抛出 {@code ProblemException}，由分派层转换为命令错误帧。
     *
     * @param context Netty 通道上下文
     * @param command 认证命令
     * @param reauth 是否为凭据替换
     * @return 已建立的认证会话
     */
    AuthenticatedSession authenticate(ChannelHandlerContext context, RealtimeClientMessage command, boolean reauth) {
        String accessToken = command.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            debugLogger.authResult(context, command, reauth, false, "unauthorized");
            throw ProblemException.of(AuthProblem.AUTHENTICATION_REQUIRED);
        }
        AccessTokenAuthenticationResult authentication = accessTokenAuthenticationApi.authenticate(accessToken);
        CpPrincipal principal = new CpPrincipal(authentication.accountId());
        replacePrincipal(context, principal);
        cancelAuthenticationTimeout(context);
        scheduleAccessTokenExpiration(context, authentication.expiresAt());
        debugLogger.authResult(context, command, reauth, true, "");
        return new AuthenticatedSession(
                authentication.accountId(),
                authentication.expiresAt(),
                serverIdentityProperties.id()
        );
    }

    /**
     * 认证成功应答载荷。
     *
     * @param session 认证会话
     * @return 应答载荷
     */
    Map<String, Object> ackData(AuthenticatedSession session) {
        return Map.of(
                "uid", Long.toString(session.accountId()),
                "expires_at", session.expiresAt().toEpochMilli(),
                "server_id", session.serverId()
        );
    }

    void scheduleAuthenticationTimeout(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        ScheduledFuture<?> timeoutFuture = context.executor().schedule(
                () -> RealtimeChannelLogContext.run(context, () -> {
                    if (context.channel().isActive() && !isAuthenticated(context)) {
                        debugLogger.frameRejected(context, "authentication_timeout", null);
                        context.writeAndFlush(errorFrame(
                                "auth",
                                "authentication_timeout",
                                "authentication frame was not received in time"
                        )).addListener(ChannelFutureListener.CLOSE);
                    }
                }),
                authenticationTimeoutSeconds,
                TimeUnit.SECONDS
        );
        context.channel().attr(RealtimeChannelSession.AUTHENTICATION_TIMEOUT_FUTURE_KEY).set(timeoutFuture);
    }

    boolean isAuthenticated(ChannelHandlerContext context) {
        return context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get() != null;
    }

    long accountId(ChannelHandlerContext context) {
        CpPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        return principal == null ? 0L : principal.accountId();
    }

    void channelInactive(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        cancelAccessTokenExpiration(context);
        debugLogger.channelInactive(context);
        CpPrincipal principal = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                .get();
        if (principal != null) {
            realtimeConnectionIndex.unregister(principal.accountId(), context.channel());
        }
    }

    private TextWebSocketFrame errorFrame(String requestType, String reason, String message) {
        return new TextWebSocketFrame(frameCodec.encode(
                new RealtimeFrame.CommandError(null, requestType, reason, message)
        ));
    }

    private void replacePrincipal(ChannelHandlerContext context, CpPrincipal principal) {
        CpPrincipal previousPrincipal = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                .get();
        if (previousPrincipal != null && previousPrincipal.accountId() != principal.accountId()) {
            realtimeConnectionIndex.unregister(previousPrincipal.accountId(), context.channel());
        }
        context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(principal);
        realtimeConnectionIndex.register(principal.accountId(), context.channel());
    }

    private void cancelAuthenticationTimeout(ChannelHandlerContext context) {
        ScheduledFuture<?> timeoutFuture = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATION_TIMEOUT_FUTURE_KEY)
                .getAndSet(null);
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private void scheduleAccessTokenExpiration(ChannelHandlerContext context, Instant expiresAt) {
        cancelAccessTokenExpiration(context);
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY).set(expiresAt);
        long delayMillis = Math.max(0L, Duration.between(timeProvider.nowInstant(), expiresAt).toMillis());
        ScheduledFuture<?> expirationFuture = context.executor().schedule(
                () -> RealtimeChannelLogContext.run(context, () -> {
                    Instant currentExpiresAt = context.channel()
                            .attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY)
                            .get();
                    if (context.channel().isActive()
                            && isAuthenticated(context)
                            && expiresAt.equals(currentExpiresAt)) {
                        debugLogger.frameRejected(context, "token_expired", null);
                        context.writeAndFlush(errorFrame("auth", "token_expired", "access token is expired"))
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                }),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRATION_FUTURE_KEY).set(expirationFuture);
    }

    private void cancelAccessTokenExpiration(ChannelHandlerContext context) {
        ScheduledFuture<?> expirationFuture = context.channel()
                .attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRATION_FUTURE_KEY)
                .getAndSet(null);
        if (expirationFuture != null) {
            expirationFuture.cancel(false);
        }
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY).set(null);
    }

    /**
     * 已认证会话。
     *
     * @param accountId 账号 ID
     * @param expiresAt 访问令牌过期时间
     * @param serverId 服务端标识
     */
    record AuthenticatedSession(long accountId, Instant expiresAt, String serverId) {
    }
}
