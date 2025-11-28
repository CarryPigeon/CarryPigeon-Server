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
 * netty网络服务启动类，用于启动网络接口用于使用
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
        try{
            log.info("netty server is starting");
            // 创建bootstrap对象，用于配置服务
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 创建boss线程组与worker线程组，用于构建反应式处理框架
            NioEventLoopGroup bossGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            // 进行具体的配置
            bootstrap
                    // 绑定线程组
                    .group(bossGroup, workerGroup)
                    // 设置为非阻塞式服务器socket通道
                    .channel(NioServerSocketChannel.class)
                    // 设置监听端口
                    .localAddress(new InetSocketAddress(config.getPort()))
                    // 配置handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加解码器，用于解码数据包
                            pipeline.addLast(new NettyDecoder());
                            // 添加编码码器，用于编码数据包
                            pipeline.addLast(new NettyEncoder());
                            // 添加心跳检测，用于客户端的保活与断线处理
                            pipeline.addLast(new IdleStateHandler(13,10,20, TimeUnit.SECONDS));
                            pipeline.addLast(new CPNettyHeartBeatHandler());
                            // 添加处理器，用于将请求分发到具体的处理器
                            pipeline.addLast(new ConnectionHandler(cpControllerDispatcher,objectMapper));
                        }
                    });
            ChannelFuture channelFuture= bootstrap.bind().sync();
            log.info("netty server started on port {}", config.getPort());
            channelFuture.channel().closeFuture().sync();
            log.info("netty server is shutting down,preparing to close bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("netty server is shut down");
        } catch (InterruptedException e) {
            log.error("netty server is interrupted,error msg:{}\n{}",e.getMessage(),e);
            // 结束程序
            System.exit(1);
        }
    }
}
