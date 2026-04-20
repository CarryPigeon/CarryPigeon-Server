package team.carrypigeon.backend.starter.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;

/**
 * Netty 实时通道运行时。
 * 职责：在启动层托管 Netty WebSocket 服务的线程组、端口绑定和关闭过程。
 * 边界：这里只负责基础设施装配，不承载任何聊天业务分发逻辑。
 */
@Slf4j
public class RealtimeServerRuntime implements SmartLifecycle {

    private final RealtimeServerProperties properties;
    private final RealtimeChannelInitializer initializer;
    private volatile boolean running;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public RealtimeServerRuntime(RealtimeServerProperties properties, RealtimeChannelInitializer initializer) {
        this.properties = properties;
        this.initializer = initializer;
    }

    @Override
    public void start() {
        if (running || !properties.enabled()) {
            return;
        }
        bossGroup = new NioEventLoopGroup(Math.max(1, properties.bossThreads()));
        workerGroup = properties.workerThreads() > 0
                ? new NioEventLoopGroup(properties.workerThreads())
                : new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer);
            serverChannel = bootstrap.bind(new InetSocketAddress(properties.host(), properties.port()))
                    .sync()
                    .channel();
            running = true;
            log.info("Realtime Netty server started on {}:{}{}", properties.host(), properties.port(), properties.path());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            shutdownGroups();
            throw new IllegalStateException("Failed to start realtime Netty server", exception);
        } catch (RuntimeException exception) {
            shutdownGroups();
            throw exception;
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
            }
        } finally {
            shutdownGroups();
            running = false;
            log.info("Realtime Netty server stopped");
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @PreDestroy
    void destroy() {
        stop();
    }

    private void shutdownGroups() {
        if (serverChannel != null) {
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
    }
}
