package team.carrypigeon.backend.connection.handler;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.protocol.aad.AeadAad;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;
import team.carrypigeon.backend.connection.security.CPAESKeyPack;
import team.carrypigeon.backend.connection.security.CPECCKeyPack;
import team.carrypigeon.backend.connection.session.NettySession;

import java.nio.charset.StandardCharsets;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

/**
 * 连接处理器：<br/>
 * 1. 负责对数据包进行解密<br/>
 * 2. 负责通过状态机获取数据包的处理状态<br/>
 * 3. 负责将解密后的数据包交给分发器进一步处理<br/>
 * */
@Slf4j
@AllArgsConstructor
public class ConnectionHandler extends SimpleChannelInboundHandler<byte[]> {

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        if (msg == null || msg.length < 32) {
            log.warn("receive invalid packet, length={}, remoteAddress={}", msg == null ? null : msg.length,
                    ctx.channel().remoteAddress());
            return;
        }

        // 解包：前 12 字节为 nonce，接下来 20 字节为 aad，剩余为密文
        byte[] nonce = new byte[12];
        byte[] aad = new byte[20];
        byte[] cipherText = new byte[msg.length - 32];
        System.arraycopy(msg, 0, nonce, 0, 12);
        System.arraycopy(msg, 12, aad, 0, 20);
        System.arraycopy(msg, 32, cipherText, 0, msg.length - 32);

        // 获取当前会话
        CPSession session = ctx.channel().attr(SESSIONS).get();
        if (session == null) {
            log.warn("receive packet without CPSession bound, remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        // 通过状态机判断是否已经完成密钥交换
        if (!session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, Boolean.class)) {
            handleAsymmetricHandshake(ctx, session, cipherText);
            return;
        }

        // 校验 aad 值是否正确，否则直接返回并中断连接
        if (!checkAAD(session, aad)) {
            Long sid = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.error("aad check failed, closing the connection, remoteAddress={}, sessionId={}",
                    ctx.channel().remoteAddress(), sid);
            session.close();
            return;
        }

        // 以下逻辑已经在 NettyConnectionStarter 中通过 bizGroup 绑定到业务线程池中执行
        // 1. 过滤心跳包
        if (isHeartbeatPacket(nonce)) {
            log.debug("receive heartbeat packet, remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        log.debug("receive encrypted packet, length={}, remoteAddress={}", cipherText.length, ctx.channel().remoteAddress());
        // 2. 解密业务数据
        String pack = decryptPayload(ctx, session, cipherText, nonce, aad);
        if (pack == null) {
            return;
        }

        // 3. 分发到业务控制器
        dispatchToController(session, pack);
    }

    /**
     * 断开连接时回调，用于进行资源清理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("connection inactive, remoteAddress={}", ctx.channel().remoteAddress());
        cpControllerDispatcher.channelInactive(ctx.channel().attr(SESSIONS).get());
        super.channelInactive(ctx);
    }

    /**
     * 建立连接时回调，用于初始化会话
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(SESSIONS).set(new NettySession(ctx));
        log.info("new connection active, remoteAddress={}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    /**
     * 处理获取到的非对称加密公钥的请求
     */
    private void receiveAsymmetry(CPSession session, String msg) {
        try {
            // 解析数据
            CPECCKeyPack cpKeyMessage = objectMapper.readValue(msg, CPECCKeyPack.class);
            if (cpKeyMessage.getId() == 0 || cpKeyMessage.getKey() == null) {
                throw new RuntimeException("illegal format");
            }

            // 生成并设置对称密钥
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY,
                    Base64.encode(AESUtil.generateKey().getEncoded()));

            // 封装返回包
            CPAESKeyPack aesPack = new CPAESKeyPack();
            aesPack.setKey(Base64.encode(ECCUtil.encrypt(
                    session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, String.class),
                    ECCUtil.rebuildPublicKey(cpKeyMessage.getKey()))));
            aesPack.setId(cpKeyMessage.getId());
            aesPack.setSessionId(session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class));
            session.write(objectMapper.writeValueAsString(aesPack), false);
            // 设置状态
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
            log.info("ECC key exchange success, sessionId={}, requestId={}",
                    session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class),
                    cpKeyMessage.getId());
        } catch (Exception e) {
            Long sessionId = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.error("asymmetric key exchange error, sessionId={}", sessionId, e);
        }
    }

    /**
     * 握手阶段处理入口。
     */
    private void handleAsymmetricHandshake(ChannelHandlerContext ctx, CPSession session, byte[] cipherText) {
        log.info("receive ECC key exchange message, remoteAddress={}", ctx.channel().remoteAddress());
        String payload = new String(cipherText, StandardCharsets.UTF_8);
        receiveAsymmetry(session, payload);
    }

    /**
     * 判断是否为心跳包（nonce 全 0）。
     */
    private boolean isHeartbeatPacket(byte[] nonce) {
        for (byte b : nonce) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解密业务数据，失败时返回 null 并关闭连接。
     */
    private String decryptPayload(ChannelHandlerContext ctx,
                                  CPSession session,
                                  byte[] cipherText,
                                  byte[] nonce,
                                  byte[] aad) {
        try {
            return AESUtil.decryptWithAAD(cipherText, nonce, aad,
                    Base64.decode(session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, String.class)));
        } catch (Exception e) {
            Long sid = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.error("decryption failed, closing the connection, remoteAddress={}, sessionId={}",
                    ctx.channel().remoteAddress(), sid);
            log.error(e.getMessage(), e);
            session.close();
            return null;
        }
    }

    /**
     * 将明文数据分发到业务控制器并回写响应。
     */
    private void dispatchToController(CPSession session, String pack) {
        CPResponse response = cpControllerDispatcher.process(pack, session);
        if (response == null) {
            return;
        }
        try {
            session.write(objectMapper.writeValueAsString(response), true);
        } catch (JsonProcessingException e) {
            Long sid = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.error("serialize CPResponse failed, sessionId={}", sid, e);
            session.close();
        }
    }

    /**
     * 对 aad 进行校验
     */
    private boolean checkAAD(CPSession session, byte[] aad) {
        if (aad == null || aad.length != AeadAad.LENGTH) {
            log.error("aad length error, length={}", aad == null ? null : aad.length);
            return false;
        }
        AeadAad aeadAad;
        try {
            aeadAad = AeadAad.decode(aad);
        } catch (IllegalArgumentException e) {
            log.error("aad decode error: {}", e.getMessage());
            return false;
        }
        int packageId = aeadAad.getPackageId();
        long aadSessionId = aeadAad.getSessionId();
        long packageTimestamp = aeadAad.getTimestampMillis();

        Integer lastPackageId = session.getAttributeValue(ConnectionAttributes.PACKAGE_ID, Integer.class);
        Long sessionId = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
        if (lastPackageId != null && packageId <= lastPackageId) {
            log.error("package sequence error, last={}, current={}, sessionId={}",
                    lastPackageId, packageId, sessionId);
            return false;
        }
        // 序列通过校验后更新
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, packageId);

        // 校验会话 id
        if (sessionId == null || aadSessionId != sessionId) {
            log.error("session id mismatch, expected={}, actual={}", sessionId, aadSessionId);
            return false;
        }

        // 校验时间戳是否在三分钟以内
        long now = System.currentTimeMillis();
        if (now - packageTimestamp > 180000) {
            log.error("package timestamp error, now={}, packageTimestamp={}, diff={}ms, sessionId={}",
                    now, packageTimestamp, now - packageTimestamp, sessionId);
            return false;
        }

        return true;
    }
}
