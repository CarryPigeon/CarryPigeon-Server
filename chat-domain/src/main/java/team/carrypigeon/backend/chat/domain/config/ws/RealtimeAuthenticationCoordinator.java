package team.carrypigeon.backend.chat.domain.config.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.config.ws.context.RealtimeContext;
import team.carrypigeon.backend.chat.domain.config.ws.downlink.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeAuthSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时连接认证状态机。
 * 职责：承接凭证校验、principal 绑定、认证超时与 token 过期定时、断连注销。
 * 边界：不解析上行帧、不承载续传逻辑；只有本类会主动写出认证类下行帧。
 */
@Slf4j
public final class RealtimeAuthenticationCoordinator {
    private final TimeProviderImpl timeProvider;
    private final RealtimeConnectionIndex realtimeConnectionIndex;
    private final AuthTokenApi authTokenApi;
    private final int authenticationTimeoutSeconds;
    private final RealtimeFrameCodec frameCodec;

    public RealtimeAuthenticationCoordinator(
        TimeProviderImpl timeProvider,
        RealtimeConnectionIndex realtimeConnectionIndex,
        AuthTokenApi authTokenApi,
        int authenticationTimeoutSeconds,
        RealtimeFrameCodec frameCodec
    ) {
        this.timeProvider = timeProvider;
        this.authTokenApi = authTokenApi;
        this.realtimeConnectionIndex = realtimeConnectionIndex;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
        this.frameCodec = frameCodec;
    }

    /**
     * 校验凭证并绑定当前连接。
     * 失败语义：凭证缺失、无效或过期时抛出 {@code ProblemException}，由分派层转换为命令错误帧。
     *
     * @param context Netty 通道上下文
     * @param accessToken 客户端 access token，可为空
     * @param requestId 客户端请求 ID，仅用于日志
     * @param reauth 是否为凭据替换
     * @return 认证会话
     */
    public RealtimeAuthSession authenticate(
            ChannelHandlerContext context,
            String accessToken,
            String requestId,
            boolean reauth
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            log.debug("Realtime authentication failed, reason=unauthorized, id={}, reauth={}", requestId, reauth);
            throw ProblemException.of(AuthProblem.AUTHENTICATION_REQUIRED);
        }
        AccessTokenAuthenticationResult authentication = authTokenApi.authenticate(accessToken);
        CpPrincipal principal = new CpPrincipal(authentication.accountId());
        replacePrincipal(context, principal);
        cancelAuthenticationTimeout(context);
        scheduleAccessTokenExpiration(context, authentication.expiresAt());
        log.debug("Realtime authentication succeeded, uid={}, id={}, reauth={}",
                authentication.accountId(), requestId, reauth);
        return new RealtimeAuthSession(
                authentication.accountId(),
                authentication.expiresAt()
        );
    }

    /**
     * 登记首帧认证超时。
     * 副作用：超时后写出认证错误帧并关闭连接。
     *
     * @param context Netty 通道上下文
     */
    public void scheduleAuthenticationTimeout(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        ScheduledFuture<?> timeoutFuture = context.executor().schedule(
                () -> {
                    if (context.channel().isActive() && !isAuthenticated(context)) {
                        log.debug("Realtime authentication timeout, closing connection");
                        context.writeAndFlush(errorFrame(
                            "auth",
                            "authentication_timeout",
                            "authentication frame was not received in time"
                        )).addListener(ChannelFutureListener.CLOSE);
                    }
                },
                authenticationTimeoutSeconds,
                TimeUnit.SECONDS
        );
        RealtimeContext.setAuthenticationTimeoutFutureKey(context.channel(), timeoutFuture);
    }

    /**
     * 当前连接是否已完成认证。
     *
     * @param context Netty 通道上下文
     * @return 已认证时返回 true
     */
    public boolean isAuthenticated(ChannelHandlerContext context) {
        return RealtimeContext.getAuthenticatedPrincipalKey(context.channel()) != null;
    }

    /**
     * 当前连接账号 ID。
     *
     * @param context Netty 通道上下文
     * @return 账号 ID；未认证时为 0
     */
    public long accountId(ChannelHandlerContext context) {
        CpPrincipal principal = RealtimeContext.getAuthenticatedPrincipalKey(context.channel());
        return principal == null ? 0L : principal.accountId();
    }

    /**
     * 连接断开时的清理。
     * 副作用：取消失效定时任务，并把当前通道从在线索引中摘除。
     *
     * @param context Netty 通道上下文
     */
    public void channelInactive(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        cancelAccessTokenExpiration(context);
        CpPrincipal principal = RealtimeContext.getAuthenticatedPrincipalKey(context.channel());
        if (principal != null) {
            realtimeConnectionIndex.unregister(principal.accountId(), context.channel());
            log.debug("Realtime connection closed, uid={}", principal.accountId());
        } else {
            log.debug("Realtime connection closed before authentication");
        }
    }

    private TextWebSocketFrame errorFrame(String requestType, String reason, String message) {
        return new TextWebSocketFrame(frameCodec.encode(
                new RealtimeFrame.CommandError(null, requestType, reason, message)
        ));
    }

    private void replacePrincipal(ChannelHandlerContext context, CpPrincipal principal) {
        CpPrincipal previousPrincipal = RealtimeContext.getAuthenticatedPrincipalKey(context.channel());
        if (previousPrincipal != null && !Objects.equals(previousPrincipal.accountId(), principal.accountId())) {
            realtimeConnectionIndex.unregister(previousPrincipal.accountId(), context.channel());
        }
        RealtimeContext.setAuthenticatedPrincipalKey(context.channel(), principal);
        realtimeConnectionIndex.register(principal.accountId(), context.channel());
    }

    private void cancelAuthenticationTimeout(ChannelHandlerContext context) {
        ScheduledFuture<?> timeoutFuture = RealtimeContext.getAuthenticationTimeoutFutureKey(context.channel());
        RealtimeContext.setAuthenticationTimeoutFutureKey(context.channel(), null);
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private void scheduleAccessTokenExpiration(ChannelHandlerContext context, Instant expiresAt) {
        cancelAccessTokenExpiration(context);
        RealtimeContext.setAccessTokenExpiresAtKey(context.channel(), expiresAt);
        long delayMillis = Math.max(0L, Duration.between(timeProvider.nowInstant(), expiresAt).toMillis());
        ScheduledFuture<?> expirationFuture = context.executor().schedule(
                () ->  {
                    Instant currentExpiresAt = RealtimeContext.getAccessTokenExpiresAtKey(context.channel());
                    if (context.channel().isActive()
                            && isAuthenticated(context)
                            && expiresAt.equals(currentExpiresAt)) {
                        log.debug("Realtime access token expired, closing connection");
                        context.writeAndFlush(errorFrame("auth", "token_expired", "access token is expired"))
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                },
                delayMillis,
                TimeUnit.MILLISECONDS
        );
        RealtimeContext.setAccessTokenExpirationFutureKey(context.channel(), expirationFuture);
    }

    private void cancelAccessTokenExpiration(ChannelHandlerContext context) {
        ScheduledFuture<?> expirationFuture = RealtimeContext.getAccessTokenExpirationFutureKey(context.channel());
        RealtimeContext.setAccessTokenExpirationFutureKey(context.channel(), null);
        if (expirationFuture != null) {
            expirationFuture.cancel(false);
        }
        RealtimeContext.setAccessTokenExpiresAtKey(context.channel(), null);
    }
}
