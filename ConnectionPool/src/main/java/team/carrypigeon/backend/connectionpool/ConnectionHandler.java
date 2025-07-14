package team.carrypigeon.backend.connectionpool;

import cn.hutool.crypto.symmetric.AES;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.connectionpool.security.CPClientSecurity;
import team.carrypigeon.backend.connectionpool.security.CPClientSecurityEnum;
import team.carrypigeon.backend.connectionpool.security.CPKeyMessage;
import team.carrypigeon.backend.connectionpool.security.aes.AESUtil;
import team.carrypigeon.backend.connectionpool.security.ecc.ECCUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler<String> {

    private CPControllerDispatcher cpControllerDispatcher;
    private ObjectMapper objectMapper;
    private static final AttributeKey<CPClientSecurity> SECURITY_STATE = AttributeKey.valueOf("SecurityState");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        ctx.channel().attr(SECURITY_STATE).setIfAbsent(new CPClientSecurity());
        CPClientSecurity security = ctx.channel().attr(SECURITY_STATE).get();
        switch (security.getState()) {
            case WAIT_ASYMMETRY:
                receiveAsymmetry(ctx, msg,security);
                return;
            case WAIT_VERIFICATION:
                receiveVerification(ctx, msg,security);
                return;
            case SUCCESS:
                // 若已经建立连接则break跳出switch进行下一步处理
                break;
        }
        try {
            ctx.writeAndFlush(objectMapper.writeValueAsString(cpControllerDispatcher.process(msg)));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 处理获取了非对称加密公钥的请求
     * TODO 优化加密算法
     * */
    private void receiveAsymmetry(ChannelHandlerContext ctx, String msg, CPClientSecurity security){
        try {
            CPKeyMessage cpKeyMessage = objectMapper.readValue(msg, CPKeyMessage.class);
            if (cpKeyMessage.getId()==0||cpKeyMessage.getKey()==null){
                throw new RuntimeException("illegal format");
            }
            // 生成密钥
            SecretKey secretKey = AESUtil.generateKey();;
            security.setKey(AESUtil.convertKeyToString(secretKey));
            CPKeyMessage message = new CPKeyMessage();
            message.setKey(ECCUtil.eccEncrypt(cpKeyMessage.getKey(),security.getKey()));
            message.setId(cpKeyMessage.getId());
            ctx.writeAndFlush(objectMapper.writeValueAsString(message));
            security.setState(CPClientSecurityEnum.WAIT_VERIFICATION);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            ctx.close();
        }
    }

    /**
     * TODO 处理获取验证消息的请求
     * */
    private void receiveVerification(ChannelHandlerContext ctx, String msg, CPClientSecurity security){
        String key = security.getKey();
        try {
            String decrypt = AESUtil.decrypt(msg, AESUtil.convertStringToKey(key));
            if(!decrypt.equals("verification")){
                log.error("decrypt error");
                ctx.close();
            }
            security.setState(CPClientSecurityEnum.SUCCESS);
            log.debug("key exchange success");
        } catch (Exception e) {
            log.error(e.getMessage());
            ctx.close();
        }
    }
}