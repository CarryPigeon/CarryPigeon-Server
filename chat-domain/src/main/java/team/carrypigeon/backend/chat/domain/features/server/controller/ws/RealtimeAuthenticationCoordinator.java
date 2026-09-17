package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时连接认证协调器。
 * 职责：执行 auth/reauth、维护账户通道注册，并管理首帧宽限与 token 到期计时。
 * 边界：不解析 JSON、不决定顶层命令类型，也不读取或映射续传事件。
 */
final class RealtimeAuthenticationCoordinator {

    private final TimeProviderImpl timeProvider;
    private final AccessTokenAuthenticationApi accessTokenAuthenticationApi;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final int authenticationTimeoutSeconds;
    private final RealtimeWebSocketDebugLogger debugLogger;
    private final RealtimeFrameCodec frameCodec;

    RealtimeAuthenticationCoordinator(
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            int authenticationTimeoutSeconds,
            RealtimeWebSocketDebugLogger debugLogger,
            RealtimeFrameCodec frameCodec
    ) {
        if (authenticationTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("authenticationTimeoutSeconds must be greater than 0");
        }
        this.timeProvider = timeProvider;
        this.accessTokenAuthenticationApi = accessTokenAuthenticationApi;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
        this.debugLogger = debugLogger;
        this.frameCodec = frameCodec;
    }

    /**
     * 认证或替换当前连接凭据。
     * 副作用：更新 principal、会话注册和 token 计时，成功响应后执行续传回调。
     */
    void authenticate(
            ChannelHandlerContext context,
            RealtimeClientMessage request,
            boolean reauth,
            Consumer<AuthenticatedAccount> afterAuthenticated
    ) {
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            debugLogger.authResult(context, request, reauth, false, "unauthorized");
            context.writeAndFlush(frameCodec.commandError(
                    request.id(),
                    reauth ? "reauth.err" : "auth.err",
                    "unauthorized",
                    "authentication is required"
            ));
            return;
        }
        try {
            AccessTokenAuthenticationResult authentication = accessTokenAuthenticationApi.authenticate(accessToken);
            AuthenticatedAccount principal = new AuthenticatedAccount(
                    authentication.accountId(),
                    authentication.username()
            );
            replacePrincipal(context, principal);
            cancelAuthenticationTimeout(context);
            scheduleAccessTokenExpiration(context, authentication.expiresAt());
            debugLogger.authResult(context, request, reauth, true, "");
            context.writeAndFlush(frameCodec.serverFrame(
                    reauth ? "reauth.ok" : "auth.ok",
                    request.id(),
                    Map.of(
                            "uid", Long.toString(authentication.accountId()),
                            "expires_at", authentication.expiresAt().toEpochMilli(),
                            "server_id", serverIdentityProperties.id()
                    ),
                    null
            ));
            afterAuthenticated.accept(principal);
        } catch (ProblemException exception) {
            String reason = frameCodec.mapReason(exception);
            debugLogger.authResult(context, request, reauth, false, reason);
            context.writeAndFlush(frameCodec.commandError(
                    request.id(),
                    reauth ? "reauth.err" : "auth.err",
                    reason,
                    exception.getMessage()
            ));
        }
    }

    void scheduleAuthenticationTimeout(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        ScheduledFuture<?> timeoutFuture = context.executor().schedule(
                () -> RealtimeChannelLogContext.run(context, () -> {
                    if (context.channel().isActive() && !isAuthenticated(context)) {
                        debugLogger.frameRejected(context, "authentication_timeout", null);
                        context.writeAndFlush(frameCodec.commandError(
                                null,
                                "auth.err",
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

    void rejectUnauthenticatedCommand(ChannelHandlerContext context, RealtimeClientMessage request) {
        debugLogger.frameRejected(context, "unauthorized", null);
        context.writeAndFlush(frameCodec.commandError(
                request.id(), "auth.err", "unauthorized", "auth must be the first realtime command"
        )).addListener(ChannelFutureListener.CLOSE);
    }

    void writeErrorAndCloseIfUnauthenticated(ChannelHandlerContext context, TextWebSocketFrame errorFrame) {
        if (isAuthenticated(context)) {
            context.writeAndFlush(errorFrame);
        } else {
            context.writeAndFlush(errorFrame).addListener(ChannelFutureListener.CLOSE);
        }
    }

    void channelInactive(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        cancelAccessTokenExpiration(context);
        debugLogger.channelInactive(context);
        AuthenticatedAccount principal = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                .get();
        if (principal != null) {
            realtimeSessionRegistry.unregister(principal.accountId(), context.channel());
        }
    }

    private void replacePrincipal(ChannelHandlerContext context, AuthenticatedAccount principal) {
        AuthenticatedAccount previousPrincipal = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                .get();
        if (previousPrincipal != null && previousPrincipal.accountId() != principal.accountId()) {
            realtimeSessionRegistry.unregister(previousPrincipal.accountId(), context.channel());
        }
        context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(principal);
        realtimeSessionRegistry.register(principal.accountId(), context.channel());
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
                        context.writeAndFlush(frameCodec.commandError(
                                null, "auth.err", "token_expired", "access token is expired"
                        )).addListener(ChannelFutureListener.CLOSE);
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
}
