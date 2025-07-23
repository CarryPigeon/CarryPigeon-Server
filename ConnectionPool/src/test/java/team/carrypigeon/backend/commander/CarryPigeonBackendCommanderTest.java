package team.carrypigeon.backend.commander;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.connectionpool.ed.ByteToJsonDecoder;
import team.carrypigeon.backend.connectionpool.ed.JsonToByteEncoder;
import team.carrypigeon.backend.connectionpool.heart.CPNettyHeartBeatHandler;
import team.carrypigeon.backend.connectionpool.heart.HeartBeatMessage;
import team.carrypigeon.backend.connectionpool.security.CPKeyMessage;
import team.carrypigeon.backend.connectionpool.security.ecc.ECCUtil;
import team.carrypigeon.backend.connectionpool.security.ecc.RsaKeyPair;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static team.carrypigeon.backend.connectionpool.ConnectionConstant.CHANNEL;

/**
 * 测试数据
 *  登录: {"id":123123,"route":"/core/account/login","data":{"email":"midream","password":"midream","device_name":"midream"}}
 *  消息发送：{"id":12323,"route":"/core/msg/text/send","data":{"to_id":1,"content":"awdadawd"}}
 * */
public class CarryPigeonBackendCommanderTest {

    public static void main(String[] args) throws InterruptedException {
        TestClientState testClientState = new TestClientState();
        EventLoopGroup eventExecutors = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventExecutors)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加解码器
                            pipeline.addLast(new ByteToJsonDecoder());
                            // 添加编码码器
                            pipeline.addLast(new JsonToByteEncoder());
                            // 添加心跳检测
                            pipeline.addLast(new IdleStateHandler(13,10,20, TimeUnit.SECONDS));
                            pipeline.addLast(new CPNettyHeartBeatHandler(new ObjectMapper()));
                            // 添加处理器
                            pipeline.addLast(new CommanderHandler(testClientState));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 7609).sync();
            new Thread(()->{
                try {
                    startConsoleInputThread(channelFuture.channel(),testClientState);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            channelFuture.channel().closeFuture().sync();
        } finally {
            eventExecutors.shutdownGracefully();
        }
    }

    public static void startConsoleInputThread(Channel channel, TestClientState testClientState) throws JsonProcessingException {
        RsaKeyPair rsaKeyPair = ECCUtil.generateEccKeyPair(256);
        testClientState.setECCKey(rsaKeyPair.getPrivateKey());
        CPKeyMessage cpKeyMessage = new CPKeyMessage();
        cpKeyMessage.setId(234234234);
        cpKeyMessage.setKey(rsaKeyPair.getPublicKey());
        channel.writeAndFlush(new ObjectMapper().writeValueAsString(cpKeyMessage));
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String s = scanner.nextLine();
            channel.writeAndFlush(s);
            CPChannel cpChannel = channel.attr(CHANNEL).get();
            if (cpChannel != null){
                cpChannel.sendMessage(s);
            }else {
                channel.writeAndFlush(s);
            }
        }
    }
}