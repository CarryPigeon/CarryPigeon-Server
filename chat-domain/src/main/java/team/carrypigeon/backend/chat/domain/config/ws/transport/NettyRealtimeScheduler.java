package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
        authTimeoutFuture = context.executor().schedule(() -> {
            Channel channel = context.channel();
            if (channel.isActive() && attributes.getAccountId() == null) {
                context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(new RealtimeFrame.CommandError(null, "auth", "authentication_timeout", "authentication frame was not received in time"))))
                    .addListener(ChannelFutureListener.CLOSE);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void scheduleAccessTokenExpiration(Instant expiresAt) {
        cancelTokenExpiration();
        currentTokenExpiresAt = expiresAt;
        long delayMillis = Math.max(0L,
            Duration.between(timeProvider.nowInstant(), expiresAt).toMillis());
        tokenExpirationFuture = context.executor().schedule(() -> {
            Channel channel = context.channel();
            if (channel.isActive()
                    && attributes.getAccountId() != null
                    && expiresAt.equals(currentTokenExpiresAt)) {
                context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(new RealtimeFrame.CommandError(null, "auth", "token_expired", "access token is expired"))))
                    .addListener(ChannelFutureListener.CLOSE);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
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
