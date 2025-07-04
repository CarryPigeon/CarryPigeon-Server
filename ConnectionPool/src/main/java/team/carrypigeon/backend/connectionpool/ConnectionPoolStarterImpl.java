package team.carrypigeon.backend.connectionpool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.connection.pool.ConnectionPoolStarter;
import team.carrypigeon.backend.connectionpool.ed.ByteToJsonDecoder;
import team.carrypigeon.backend.connectionpool.ed.JsonToByteEncoder;

import java.net.InetSocketAddress;

/**
 * netty网络服务启动类，用于启动网络接口用于使用
 * @author midreamsheep
 * */
@Component
@Slf4j
public class ConnectionPoolStarterImpl implements ConnectionPoolStarter {

    private final CPControllerDispatcher cpControllerDispatcher;

    @Autowired
    public ConnectionPoolStarterImpl(CPControllerDispatcher cpControllerDispatcher) {
        this.cpControllerDispatcher = cpControllerDispatcher;
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
                    // 添加解码器
                    ch.pipeline().addLast(new ByteToJsonDecoder());
                    // 添加处理器
                    ch.pipeline().addLast(new ConnectionHandler(cpControllerDispatcher));
                    // 添加编码码器
                    ch.pipeline().addLast(new JsonToByteEncoder());
                }
            });
            log.info("netty server started on port {}", port);
            ChannelFuture channelFuture= bootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
            log.info("netty server is shut down");
        } catch (InterruptedException e) {
            log.error("netty server is interrupted,error msg:{}",e.getMessage());
        }
    }
}
