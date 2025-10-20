package team.carrypigeon.backend.connectionpool.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.connectionpool.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connectionpool.channel.NettySession;
import team.carrypigeon.backend.connectionpool.security.CPEncryptionState;
import team.carrypigeon.backend.connectionpool.security.CPEncryptionStateEnum;
import team.carrypigeon.backend.connectionpool.security.CPKeyMessage;
import team.carrypigeon.backend.connectionpool.protocol.encryption.aes.AESUtil;

import javax.crypto.SecretKey;

import static team.carrypigeon.backend.connectionpool.attribute.ConnectionAttributes.SESSIONS;

/**
 * 连接处理器，用于处理与客户端的连接与对数据进行加密解密处理
 * @author midreamsheep
 * */
@AllArgsConstructor
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler<byte[]> {

    private CPControllerDispatcher cpControllerDispatcher;
    private ObjectMapper objectMapper;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        ctx.channel().attr(SESSIONS).setIfAbsent(new NettySession(ctx));
        CPSession session = ctx.channel().attr(SESSIONS).get();
        CPEncryptionState encryption = session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, CPEncryptionState.class).get();
        switch (encryption.getState()) {
            case WAIT_ASYMMETRY:
                receiveAsymmetry(ctx, msg,encryption);
                return;
            case WAIT_VERIFICATION:
                receiveVerification(ctx, msg,encryption);
                return;
            case SUCCESS:
                // 若已经建立连接则break跳出switch进行下一步处理
                break;
        }
        CPSession cpChannel = ctx.channel().attr(SESSIONS).get();
        // 提交任务
        ctx.channel().eventLoop().execute(()->{
                try {
                    CPResponse response = cpControllerDispatcher.process(AESUtil.decrypt(msg, AESUtil.convertStringToKey(encryption.getKey())), cpChannel);
                    if (response!=null){
                        cpChannel.write(objectMapper.writeValueAsString(response));
                    }
                } catch (JsonProcessingException e) {
                    try {
                        log.error("illegal json pattern;json:{}",AESUtil.decrypt(msg, AESUtil.convertStringToKey(encryption.getKey())));
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    log.error("unexpected Exception:{}",e.getMessage(),e);
                }
        }
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 回调监听事件
        cpControllerDispatcher.channelInactive(ctx.channel().attr(SESSIONS).get());
    }

    /**
     * 处理获取了非对称加密公钥的请求
     * */
    private void receiveAsymmetry(ChannelHandlerContext ctx, String msg, CPEncryptionState security){
        try {
            CPKeyMessage cpKeyMessage = objectMapper.readValue(msg, CPKeyMessage.class);
            if (cpKeyMessage.getId()==0||cpKeyMessage.getKey()==null){
                throw new RuntimeException("illegal format");
            }
            // 生成密钥
            SecretKey secretKey = AESUtil.generateKey();;
            security.setKey(AESUtil.convertKeyToString(secretKey));
            CPKeyMessage message = new CPKeyMessage();
            //message.setKey(ECCUtil.eccEncrypt(cpKeyMessage.getKey(),security.getKey()));
            message.setId(cpKeyMessage.getId());
            ctx.writeAndFlush(objectMapper.writeValueAsString(message));
            security.setState(CPEncryptionStateEnum.WAIT_VERIFICATION);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            ctx.close();
        }
    }

    /**
     * TODO 处理获取验证消息的请求
     * */
    private void receiveVerification(ChannelHandlerContext ctx, String msg, CPEncryptionState security){
        String key = security.getKey();
        try {
            String decrypt = AESUtil.decrypt(msg, AESUtil.convertStringToKey(key));
            if(!decrypt.equals("verification")){
                log.error("decrypt error");
                ctx.close();
            }
            security.setState(CPEncryptionStateEnum.SUCCESS);
            log.debug("key exchange success");
        } catch (Exception e) {
            log.error(e.getMessage());
            ctx.close();
        }
    }
}