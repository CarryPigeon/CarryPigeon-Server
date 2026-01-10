package team.carrypigeon.backend.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.connection.handler.ConnectionHandler;
import team.carrypigeon.backend.connection.heart.CPNettyHeartBeatHandler;
import team.carrypigeon.backend.connection.protocol.codec.NettyDecoder;
import team.carrypigeon.backend.connection.protocol.codec.NettyEncoder;
import team.carrypigeon.backend.connection.security.EccServerKeyHolder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * netty缃戠粶鏈嶅姟鍚姩绫伙紝鐢ㄤ簬鍚姩缃戠粶鎺ュ彛鐢ㄤ簬浣跨敤
 * @author midreamsheep
 * */
@Component
@ConditionalOnProperty(prefix = "connection", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NettyConnectionStarter {

    private static final Logger log = LoggerFactory.getLogger(NettyConnectionStarter.class);

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;
    private final ConnectionConfig config;
    private final EccServerKeyHolder eccServerKeyHolder;

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile EventExecutorGroup bizGroup;
    private volatile Channel serverChannel;
    private volatile Thread serverThread;

    @Autowired
    public NettyConnectionStarter(CPControllerDispatcher cpControllerDispatcher,
                                  ObjectMapper objectMapper,
                                  ConnectionConfig config,
                                  EccServerKeyHolder eccServerKeyHolder) {
        this.cpControllerDispatcher = cpControllerDispatcher;
        this.objectMapper = objectMapper;
        this.config = config;
        this.eccServerKeyHolder = eccServerKeyHolder;
    }

    @PostConstruct
    public void start() {
        Thread t = new Thread(this::run, "cp-netty-server");
        t.setDaemon(true);
        this.serverThread = t;
        t.start();
    }

    @PreDestroy
    public void stop() {
        try {
            Channel ch = this.serverChannel;
            if (ch != null) {
                ch.close();
            }
        } catch (Exception e) {
            log.warn("failed to close netty server channel during shutdown", e);
        }
        try {
            EventLoopGroup bg = this.bossGroup;
            if (bg != null) {
                bg.shutdownGracefully();
            }
        } catch (Exception e) {
            log.warn("failed to shutdown bossGroup during shutdown", e);
        }
        try {
            EventLoopGroup wg = this.workerGroup;
            if (wg != null) {
                wg.shutdownGracefully();
            }
        } catch (Exception e) {
            log.warn("failed to shutdown workerGroup during shutdown", e);
        }
        try {
            EventExecutorGroup eg = this.bizGroup;
            if (eg != null) {
                eg.shutdownGracefully();
            }
        } catch (Exception e) {
            log.warn("failed to shutdown bizGroup during shutdown", e);
        }
        Thread t = this.serverThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public void run() {
        // 创建线程池提前，确保 finally 中一定能够 shutdown
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        int bizThreads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(bizThreads);
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.bizGroup = bizGroup;
        try {
            log.info("netty server is starting on port {}", config.getPort());
            // 构建 bootstrap
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(config.getPort()))
                    .childHandler(new ChildChannelInitializer(this, bizGroup));
            ChannelFuture channelFuture = bind(bootstrap);
            this.serverChannel = channelFuture.channel();
            log.info("netty server started on port {}", config.getPort());
            channelFuture.channel().closeFuture().sync();
            log.info("netty server channel closed, preparing to shutdown event loops");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("netty server was interrupted, message={}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("netty server unexpected error", e);
        } finally {
            log.info("shutting down bossGroup, workerGroup and bizGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bizGroup.shutdownGracefully();
            this.serverChannel = null;
            log.info("netty server shutdown sequence initiated");
        }
    }

    ChannelFuture bind(ServerBootstrap bootstrap) throws InterruptedException {
        return bootstrap.bind().sync();
    }

    void initChildPipeline(ChannelPipeline pipeline, EventExecutorGroup bizGroup) {
        // 编解码
        pipeline.addLast(new NettyDecoder());
        pipeline.addLast(new NettyEncoder());
        // 心跳检测
        pipeline.addLast(new IdleStateHandler(13, 10, 20, TimeUnit.SECONDS));
        pipeline.addLast(new CPNettyHeartBeatHandler());
        // 业务处理，绑定到业务线程池
        pipeline.addLast(bizGroup, new ConnectionHandler(
                cpControllerDispatcher,
                objectMapper,
                eccServerKeyHolder.getPrivateKey()
        ));
    }

    static final class ChildChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final NettyConnectionStarter starter;
        private final EventExecutorGroup bizGroup;

        ChildChannelInitializer(NettyConnectionStarter starter, EventExecutorGroup bizGroup) {
            this.starter = starter;
            this.bizGroup = bizGroup;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            starter.initChildPipeline(ch.pipeline(), bizGroup);
        }
    }
}
