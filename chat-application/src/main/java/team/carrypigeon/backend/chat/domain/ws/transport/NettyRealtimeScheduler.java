package team.carrypigeon.backend.chat.domain.ws.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogScope;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrameCodec;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeAttributes;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeScheduler;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class NettyRealtimeScheduler implements RealtimeScheduler {

    private final ChannelHandlerContext context;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAttributes attributes;
    private final TimeProvider timeProvider;
    private ScheduledFuture<?> authTimeoutFuture;
    private ScheduledFuture<?> tokenExpirationFuture;
    private Instant currentTokenExpiresAt;

    public NettyRealtimeScheduler(
        ChannelHandlerContext context,
        RealtimeFrameCodec frameCodec,
        RealtimeAttributes attributes,
        TimeProvider timeProvider
    ) {
        this.attributes = attributes;
        this.context = context;
        this.frameCodec = frameCodec;
        this.timeProvider = timeProvider;
    }

    @Override
    public void scheduleAuthenticationTimeout(int timeoutSeconds) {
        cancelAuthTimeout();
        authTimeoutFuture = context.executor().schedule(LogContexts.wrap(LogScope.WS, () -> {
            Channel channel = context.channel();
            if (channel.isActive() && attributes.getAccountId() == null) {
                LogContexts.uid(null);
                log.debug("Realtime authentication timeout, channelId={}", channel.id().asShortText());
                NettyRealtimeConnection.write(context, frameCodec.encode(new RealtimeFrame.CommandError(
                    null, "auth", AuthProblem.AUTHENTICATION_REQUIRED.code(), "authentication frame was not received in time")))
                    .addListener(ChannelFutureListener.CLOSE);
            }
        }), timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void scheduleAccessTokenExpiration(Instant expiresAt) {
        cancelTokenExpiration();
        currentTokenExpiresAt = expiresAt;
        long delayMillis = Math.max(0L,
            Duration.between(timeProvider.nowInstant(), expiresAt).toMillis());
        tokenExpirationFuture = context.executor().schedule(LogContexts.wrap(LogScope.WS, () -> {
            Channel channel = context.channel();
            if (channel.isActive()
                    && attributes.getAccountId() != null
                    && expiresAt.equals(currentTokenExpiresAt)) {
                LogContexts.uid(String.valueOf(attributes.getAccountId()));
                log.debug("Realtime access token expired, channelId={}, uid={}",
                        channel.id().asShortText(), attributes.getAccountId());
                NettyRealtimeConnection.write(context, frameCodec.encode(new RealtimeFrame.CommandError(
                    null, "auth", AuthProblem.TOKEN_EXPIRED.code(), "access token is expired")))
                    .addListener(ChannelFutureListener.CLOSE);
            }
        }), delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancelAuthTimeout() {
        if (authTimeoutFuture != null) {
            authTimeoutFuture.cancel(false);
            authTimeoutFuture = null;
        }
    }

    @Override
    public void cancelTokenExpiration() {
        if (tokenExpirationFuture != null) {
            tokenExpirationFuture.cancel(false);
            tokenExpirationFuture = null;
        }
        currentTokenExpiresAt = null;
    }

}
