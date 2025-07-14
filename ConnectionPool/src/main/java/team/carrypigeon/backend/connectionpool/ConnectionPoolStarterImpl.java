package team.carrypigeon.backend.connectionpool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.connection.pool.ConnectionPoolStarter;
import team.carrypigeon.backend.connectionpool.ed.ByteToJsonDecoder;
import team.carrypigeon.backend.connectionpool.ed.JsonToByteEncoder;
import team.carrypigeon.backend.connectionpool.heart.CPNettyHeartBeatHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * netty网络服务启动类，用于启动网络接口用于使用
 * @author midreamsheep
 * */
@Component
@Slf4j
public class ConnectionPoolStarterImpl implements ConnectionPoolStarter {

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionPoolStarterImpl(CPControllerDispatcher cpControllerDispatcher, ObjectMapper objectMapper) {
        this.cpControllerDispatcher = cpControllerDispatcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(int port) {
        try{
            log.info("netty server is starting");
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    // 添加解码器
                    pipeline.addLast(new ByteToJsonDecoder());
                    // 添加编码码器
                    pipeline.addLast(new JsonToByteEncoder());
                    // 添加心跳检测
                    pipeline.addLast(new IdleStateHandler(10,10,20, TimeUnit.SECONDS));
                    pipeline.addLast(new CPNettyHeartBeatHandler(objectMapper));
                    // 添加处理器
                    pipeline.addLast(new ConnectionHandler(cpControllerDispatcher,objectMapper));
                }
            });
            log.info("netty server started on port {}", port);
            ChannelFuture channelFuture= bootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
            log.info("netty server is shut down");
        } catch (InterruptedException e) {
            log.error("netty server is interrupted,error msg:{}\n{}",e.getMessage(),e);
        }
    }
}
