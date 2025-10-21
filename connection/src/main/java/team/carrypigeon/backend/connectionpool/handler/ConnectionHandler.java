package team.carrypigeon.backend.connectionpool.handler;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.connectionpool.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connectionpool.channel.NettySession;
import team.carrypigeon.backend.connectionpool.protocol.encryption.ecc.ECCUtil;
import team.carrypigeon.backend.connectionpool.security.CPAESKeyPack;
import team.carrypigeon.backend.connectionpool.security.CPECCKeyPack;
import team.carrypigeon.backend.connectionpool.protocol.encryption.aes.AESUtil;

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
        // 解密数据
        // 前12字节为nonce，接下来20字节为aad，中间为密文
        byte[] nonce = new byte[12];
        byte[] aad = new byte[20];
        byte[] cipherText = new byte[msg.length-32];
        System.arraycopy(msg,0,nonce,0,12);
        System.arraycopy(msg,12,aad,0,20);
        System.arraycopy(msg,32,cipherText,0,msg.length-32);

        // 若不存在session则创建一个session
        ctx.channel().attr(SESSIONS).setIfAbsent(new NettySession(ctx));
        // 获取当前会话
        CPSession session = ctx.channel().attr(SESSIONS).get();

        // 通过状态机
        if (session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE,Boolean.class)) {
            // 等待非对称加密公钥
            receiveAsymmetry(session, new String(cipherText));
            return;
        }

        // 提交任务
        try (EventExecutor executor = ctx.executor()) {
            executor.execute(()->{
                // 解密数据
                String pack;
                try {
                    pack = AESUtil.decryptWithAAD(cipherText, nonce, aad, Base64.decode(session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY,String.class)));
                } catch (Exception e) {
                    log.error("decryption failed,closing the connection...");
                    log.error(e.getMessage(),e);
                    session.close();
                    return;
                }
                CPResponse response = cpControllerDispatcher.process(pack,session);
                if (response!=null){
                    try {
                        session.write(objectMapper.writeValueAsString(response),true);
                    } catch (JsonProcessingException e) {
                        log.error("it should not happen,please check the plugins or submit an issue to us!!!");
                        log.error(e.getMessage(),e);
                        session.close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 回调监听事件
        cpControllerDispatcher.channelInactive(ctx.channel().attr(SESSIONS).get());
    }

    /**
     * 处理获取了非对称加密公钥的请求
     * */
    private void receiveAsymmetry(CPSession session, String msg){
        try {
            // 解析数据
            CPECCKeyPack cpKeyMessage = objectMapper.readValue(msg, CPECCKeyPack.class);
            if (cpKeyMessage.getId()==0||cpKeyMessage.getKey()==null){
                throw new RuntimeException("illegal format");
            }

            // 生成并设置密钥
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY,Base64.encode(AESUtil.generateKey().getEncoded()));

            // 组装发送包
            CPAESKeyPack aesPack = new CPAESKeyPack();
            aesPack.setKey(Base64.encode(ECCUtil.encrypt(session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY,String.class),ECCUtil.rebuildPublicKey(cpKeyMessage.getKey()))));
            aesPack.setId(cpKeyMessage.getId());
            aesPack.setId(session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID,Long.class));

            session.write(objectMapper.writeValueAsString(aesPack),false);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }
}