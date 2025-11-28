package team.carrypigeon.backend.connection.handler;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ByteUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.session.NettySession;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;
import team.carrypigeon.backend.connection.security.CPAESKeyPack;
import team.carrypigeon.backend.connection.security.CPECCKeyPack;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;

import java.nio.ByteOrder;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

/**
 * 连接处理器：<br/>
 * 1. 负责对数据包的解码操作<br/>
 * 2. 负责通过状态机获取数据包的处理状态<br/>
 * 3. 负责将数据包解密后托付给分发器进行进一步的处理<br/>
 * @author midreamsheep
 * */
@Slf4j
@AllArgsConstructor
public class ConnectionHandler extends SimpleChannelInboundHandler<byte[]> {

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        // 解密数据
        // 前12字节为nonce，接下来20字节为aad，最后为密文
        byte[] nonce = new byte[12];
        byte[] aad = new byte[20];
        byte[] cipherText = new byte[msg.length-32];
        System.arraycopy(msg,0,nonce,0,12);
        System.arraycopy(msg,12,aad,0,20);
        System.arraycopy(msg,32,cipherText,0,msg.length-32);
        // 获取当前会话
        CPSession session = ctx.channel().attr(SESSIONS).get();
        // 通过状态机
        if (!session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE,Boolean.class)) {
            // 等待非对称加密公钥
            receiveAsymmetry(session, new String(cipherText));
            return;
        }
        // 校验aad值是否正确，否则直接返回并中断连接
        if(!checkAAD(session,aad)){
            log.error("aad check failed,closing the connection...");
            session.close();
            return;
        }
        // 提交任务
        try (EventExecutor executor = ctx.executor()) {
            executor.execute(()->{
                // 检查是否处于加密状态
                // 心跳包等属于非加密状态，若nonce为空则判定为心跳包
                boolean isEmpty = true;
                for (byte b : nonce) {
                    if (b!=0){
                        isEmpty = false;
                        break;
                    }
                }
                // 如果为空则判定为心跳包，不做处理直接返回
                if (isEmpty){
                    return;
                }
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

    /**
     * 断开连接时调用用于进行数据清理
     * */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 回调监听事件
        cpControllerDispatcher.channelInactive(ctx.channel().attr(SESSIONS).get());
        super.channelInactive(ctx);
    }

    /**
     * 建立连接时调用用于注册服务会话
     * */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 创建session会话
        ctx.channel().attr(SESSIONS).set(new NettySession(ctx));
        super.channelActive(ctx);
    }

    /**
     * 处理获取了非对称加密公钥的请求
     * @param session 当前会话
     * @param msg 收到的消息，消息的格式应该与{@link CPECCKeyPack}一致
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
            aesPack.setSessionId(session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID,Long.class));
            session.write(objectMapper.writeValueAsString(aesPack),false);
            // 设置状态
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE,true);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    /**
     * 对aad文件进行校验
     * @param session 当前会话
     * @param aad 收到的aad信息，应该长度为20字节<br/>
     *            4字节的包序列，8字节的会话id，8字节的包时间
     * */
    private boolean checkAAD(CPSession session,byte[] aad){
        // 获取包序列
        int packageId = ByteUtil.bytesToInt(aad, 0, ByteOrder.BIG_ENDIAN);
        // 判断包序列是否转置
        Integer attributeValue = session.getAttributeValue(ConnectionAttributes.PACKAGE_ID, Integer.class);
        if (attributeValue>packageId) {
            log.error("package sequence error");
            return false;
        }
        // 通过校验则更新包序列
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, packageId);

        // 获取会话id
        long sessionID = ByteUtil.bytesToLong(aad, 4, ByteOrder.BIG_ENDIAN);
        // 判断会话id是否一致
        if (sessionID!= session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class)){
            log.error("session id id error");
            return false;
        }

        // 获取包时间
        long packageTimestamp = ByteUtil.bytesToLong(aad, 12, ByteOrder.BIG_ENDIAN);
        // 判断包时间是否在三分钟以内
        if (System.currentTimeMillis()-packageTimestamp>180000){
            log.error("package timestamp error");
            return false;
        }

        return true;
    }
}