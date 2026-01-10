package team.carrypigeon.backend.connection.handler;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPConnectionAttributes;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import com.fasterxml.jackson.databind.node.ObjectNode;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.disconnect.DisconnectSupport;
import team.carrypigeon.backend.connection.protocol.aad.AeadAad;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;
import team.carrypigeon.backend.connection.security.CPAESKeyPack;
import team.carrypigeon.backend.connection.session.NettySession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import static team.carrypigeon.backend.connection.attribute.ConnectionAttributes.SESSIONS;

/**
 * 连接处理器：<br/>
 * 1. 负责对数据包进行解密<br/>
 * 2. 负责通过状态机获取数据包的处理状态<br/>
 * 3. 负责将解密后的数据包交给分发器进一步处理<br/>
 * */
public class ConnectionHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private final CPControllerDispatcher cpControllerDispatcher;
    private final ObjectMapper objectMapper;
    /**
     * 服务端 ECC 私钥，用于解密客户端上传的 AES 会话密钥。
     */
    private final PrivateKey serverPrivateKey;

    public ConnectionHandler(CPControllerDispatcher cpControllerDispatcher,
                             ObjectMapper objectMapper,
                             PrivateKey serverPrivateKey) {
        this.cpControllerDispatcher = cpControllerDispatcher;
        this.objectMapper = objectMapper;
        this.serverPrivateKey = serverPrivateKey;
    }

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
        if (!checkAAD(ctx.channel(), session, aad)) {
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
        dispatchToController(ctx, session, pack);
    }

    /**
     * 断开连接时回调，用于进行资源清理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CPSession session = ctx.channel().attr(SESSIONS).get();
        if (!DisconnectSupport.isDisconnectLogged(ctx.channel())) {
            DisconnectSupport.markDisconnectLogged(ctx.channel());
            DisconnectSupport.DisconnectInfo info = DisconnectSupport.resolveDisconnectInfo(ctx.channel(), session);
            String remoteAddress = getRemoteAddressForLog(ctx, session);
            String channelId = ctx.channel().id() == null ? null : ctx.channel().id().asShortText();
            Long sessionId = session == null ? null : session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            Boolean encrypted = session == null ? null : session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, Boolean.class);
            String reason = info.reason() == null ? "remote_closed" : info.reason();
            log.info(
                    "connection disconnected, reason={}, remoteAddress={}, channelId={}, sessionId={}, encrypted={}, causeType={}, causeMessage={}",
                    reason,
                    remoteAddress,
                    channelId,
                    sessionId,
                    encrypted,
                    info.causeType(),
                    info.causeMessage()
            );
        }
        cpControllerDispatcher.channelInactive(session);
        super.channelInactive(ctx);
    }

    /**
     * 建立连接时回调，用于初始化会话
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettySession session = new NettySession(ctx);
        // 记录基础连接信息，供上层通过 CPFlowContext 读取
        Object remote = ctx.channel().remoteAddress();
        String remoteAddress = remote != null ? remote.toString() : null;
        String remoteIp = null;
        Integer remotePort = null;
        if (remote instanceof InetSocketAddress inetSocketAddress) {
            if (inetSocketAddress.getAddress() != null) {
                remoteIp = inetSocketAddress.getAddress().getHostAddress();
            }
            remotePort = inetSocketAddress.getPort();
        }
        session.setAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, remoteAddress);
        if (remoteIp != null) {
            session.setAttributeValue(CPConnectionAttributes.REMOTE_IP, remoteIp);
        }
        if (remotePort != null) {
            session.setAttributeValue(CPConnectionAttributes.REMOTE_PORT, remotePort);
        }

        ctx.channel().attr(SESSIONS).set(session);
        log.info("new connection active, remoteAddress={}", remoteAddress);
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        CPSession session = ctx.channel().attr(SESSIONS).get();
        Throwable rootCause = DisconnectSupport.rootCause(cause);
        String reason = isConnectionReset(rootCause) ? "connection_reset" : "exception";
        DisconnectSupport.markDisconnect(ctx.channel(), session, reason, rootCause == null ? cause : rootCause);

        ctx.close();
    }

    /**
     * 处理密钥交换请求：
     * 客户端使用预先约定的服务端公钥加密 AES 会话密钥，
     * 将密文通过 {@link CPAESKeyPack} 发送到服务端。
     * 服务端使用私钥解密得到 AES 密钥，并写入会话属性。
     */
    private void receiveAsymmetry(CPSession session, String msg) {
        try {
            // 解析客户端发送的 AES 密钥包
            CPAESKeyPack aesKeyPack = objectMapper.readValue(msg, CPAESKeyPack.class);
            if ( aesKeyPack.getKey() == null) {
                throw new RuntimeException("illegal format");
            }

            // 解密得到客户端生成的 AES 会话密钥（Base64 字符串）
            byte[] encryptedKeyBytes = Base64.decode(aesKeyPack.getKey());
            String aesKeyBase64 = ECCUtil.decrypt(encryptedKeyBytes, serverPrivateKey);
            if (aesKeyBase64 == null || aesKeyBase64.isEmpty()) {
                throw new RuntimeException("empty aes key");
            }

            // 写入会话属性，后续业务数据解密将使用该密钥
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, aesKeyBase64);

            // 设置状态
            session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
            Long sessionId = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.info("AES key exchange success, sessionId={}, requestId={}", sessionId, aesKeyPack.getId());

            // 通过一个 route=handshake 的推送告知客户端握手成功
            CPNotification notification = new CPNotification();
            notification.setRoute("handshake");
            ObjectNode dataNode = objectMapper.createObjectNode();
            if (sessionId != null) {
                dataNode.put("session_id", sessionId);
                dataNode.put("sessionId", sessionId);
            }
            notification.setData(dataNode);

            CPResponse handshakeResp = new CPResponse()
                    .setId(-1)
                    .setCode(0)
                    .setData(objectMapper.valueToTree(notification));
            // 使用刚协商好的 AES 密钥加密发送，验证链路可用
            session.write(objectMapper.writeValueAsString(handshakeResp), true);
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
            DisconnectSupport.markDisconnect(ctx.channel(), session, "decrypt_failed", e);
            session.close();
            return null;
        }
    }

    /**
     * 将明文数据分发到业务控制器并回写响应。
     */
    private void dispatchToController(ChannelHandlerContext ctx, CPSession session, String pack) {
        CPResponse response = cpControllerDispatcher.process(pack, session);
        if (response == null) {
            return;
        }
        try {
            session.write(objectMapper.writeValueAsString(response), true);
        } catch (JsonProcessingException e) {
            DisconnectSupport.markDisconnect(ctx.channel(), session, "serialize_failed", e);
            session.close();
        }
    }

    /**
     * 对 aad 进行校验
     */
    private boolean checkAAD(Channel channel, CPSession session, byte[] aad) {
        if (aad == null || aad.length != AeadAad.LENGTH) {
            DisconnectSupport.markDisconnect(channel, session, "aad_length_error", "AadLengthError", "length=" + (aad == null ? null : aad.length));
            return false;
        }
        AeadAad aeadAad;
        try {
            aeadAad = AeadAad.decode(aad);
        } catch (IllegalArgumentException e) {
            DisconnectSupport.markDisconnect(channel, session, "aad_decode_error", e);
            return false;
        }
        int packageId = aeadAad.getPackageId();
        long aadSessionId = aeadAad.getSessionId();
        long packageTimestamp = aeadAad.getTimestampMillis();

        Integer lastPackageId = session.getAttributeValue(ConnectionAttributes.PACKAGE_ID, Integer.class);
        Long sessionId = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
        if (lastPackageId != null && packageId <= lastPackageId) {
            DisconnectSupport.markDisconnect(
                    channel,
                    session,
                    "package_sequence_error",
                    "PackageSequenceError",
                    "last=" + lastPackageId + ", current=" + packageId + ", sessionId=" + sessionId
            );
            return false;
        }
        // 序列通过校验后更新
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, packageId);

        // 校验会话 id
        if (sessionId == null || aadSessionId != sessionId) {
            DisconnectSupport.markDisconnect(
                    channel,
                    session,
                    "session_id_mismatch",
                    "SessionIdMismatch",
                    "expected=" + sessionId + ", actual=" + aadSessionId
            );
            return false;
        }

        // 校验时间戳是否在三分钟以内
        long now = System.currentTimeMillis();
        if (now - packageTimestamp > 180000) {
            DisconnectSupport.markDisconnect(
                    channel,
                    session,
                    "package_timestamp_error",
                    "PackageTimestampError",
                    "now=" + now + ", packageTimestamp=" + packageTimestamp + ", diffMs=" + (now - packageTimestamp) + ", sessionId=" + sessionId
            );
            return false;
        }

        return true;
    }

    private static String getRemoteAddressForLog(ChannelHandlerContext ctx, CPSession session) {
        String remoteAddress = session == null ? null : session.getAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, String.class);
        if (remoteAddress != null) {
            return remoteAddress;
        }
        Object remote = ctx.channel().remoteAddress();
        return remote == null ? null : remote.toString();
    }

    private static boolean isConnectionReset(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        if (lower.contains("connection reset")
                || lower.contains("reset by peer")
                || lower.contains("broken pipe")
                || lower.contains("forcibly closed")) {
            return (throwable instanceof SocketException) || (throwable instanceof IOException);
        }
        return false;
    }
}
