package team.carrypigeon.backend.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.connection.handler.ConnectionHandler;
import team.carrypigeon.backend.connection.protocol.codec.NettyDecoder;
import team.carrypigeon.backend.connection.protocol.codec.NettyEncoder;
import team.carrypigeon.backend.connection.heart.CPNettyHeartBeatHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * netty缃戠粶鏈嶅姟鍚姩绫伙紝鐢ㄤ簬鍚姩缃戠粶鎺ュ彛鐢ㄤ簬浣跨敤
 * @author midreamsheep
 * */
@Component
@Slf4j
public class NettyConnectionStarter {

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;
    private final ConnectionConfig config;

    @Autowired
    public NettyConnectionStarter(CPControllerDispatcher cpControllerDispatcher, ObjectMapper objectMapper, ConnectionConfig config) {
        this.cpControllerDispatcher = cpControllerDispatcher;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    @PostConstruct
    public void run() {
        // 创建线程池提前，确保 finally 中一定能够 shutdown
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        int bizThreads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(bizThreads);
        try {
            log.info("netty server is starting on port {}", config.getPort());
            // 构建 bootstrap
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(config.getPort()))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 编解码
                            pipeline.addLast(new NettyDecoder());
                            pipeline.addLast(new NettyEncoder());
                            // 心跳检测
                            pipeline.addLast(new IdleStateHandler(13, 10, 20, TimeUnit.SECONDS));
                            pipeline.addLast(new CPNettyHeartBeatHandler());
                            // 业务处理，绑定到业务线程池
                            pipeline.addLast(bizGroup, new ConnectionHandler(cpControllerDispatcher, objectMapper));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind().sync();
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
            log.info("netty server shutdown sequence initiated");
        }
    }
}
