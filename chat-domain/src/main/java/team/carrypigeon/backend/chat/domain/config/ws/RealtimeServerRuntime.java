package team.carrypigeon.backend.chat.domain.config.ws;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Netty 实时通道运行时。
 * 职责：托管实时 WebSocket 服务的线程组、端口绑定和关闭过程。
 * 边界：只负责运行时生命周期，不承载协议装配与任何业务分发逻辑。
 */
@Component
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

    /**
     * 启动 Netty realtime 运行时。
     * 职责：根据配置初始化线程组、绑定监听端口并注册 channel initializer。
     * 副作用：会创建线程资源并占用网络端口。
     * 约束：当 runtime 已启动或 realtime 被禁用时直接跳过。
     */
    @Override
    public void start() {
        if (running) {
            log.info("Realtime Netty server start skipped because runtime is already running");
            return;
        }
        if (!properties.isEnabled()) {
            log.info(
                    "Realtime Netty server start skipped because realtime is disabled (host={}, port={})",
                    properties.getHost(),
                    properties.getPort()
            );
            return;
        }
        log.info(
                "Starting realtime Netty server on {}:{} with bossThreads={} workerThreads={}",
                properties.getHost(),
                properties.getPort(),
                properties.getBossThreads(),
                properties.getWorkerThreads()
        );
        bossGroup = new NioEventLoopGroup(Math.max(1, properties.getBossThreads()));
        workerGroup = properties.getWorkerThreads() > 0
                ? new NioEventLoopGroup(properties.getWorkerThreads())
                : new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer);
            serverChannel = bootstrap.bind(new InetSocketAddress(properties.getHost(), properties.getPort()))
                    .sync()
                    .channel();
            running = true;
            log.info("Realtime Netty server started on {}:{}", properties.getHost(), properties.getPort());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            shutdownGroups();
            throw new IllegalStateException("Failed to start realtime Netty server", exception);
        } catch (RuntimeException exception) {
            shutdownGroups();
            throw exception;
        }
    }

    /**
     * 停止 Netty realtime 运行时。
     * 副作用：关闭服务端通道并回收 boss / worker 线程组。
     * 约束：重复停止是幂等的。
     */
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

    /**
     * 按 `SmartLifecycle` 契约停止运行时并在完成后回调。
     * 原因：让 Spring 容器能够在关闭阶段继续串联后续生命周期动作。
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * 返回 realtime 运行时当前是否已完成绑定并处于运行中。
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 声明 realtime runtime 由 Spring 容器自动启动。
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 返回 runtime 的生命周期阶段。
     * 约束：使用最大 phase，确保其在大多数基础 Bean 完成后再启动。
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @PreDestroy
    void destroy() {
        stop();
    }

    /**
     * 关闭 Netty 运行时资源。
     * 副作用：清理 server channel 引用，并同步关闭 boss/worker event loop group，避免后台线程泄漏。
     */
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
