package team.carrypigeon.backend.connection.session;

import cn.hutool.core.codec.Base64;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.disconnect.DisconnectSupport;
import team.carrypigeon.backend.connection.protocol.aad.AeadAad;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESData;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 会话实现，对 {@link ChannelHandlerContext} 进行封装，
 * 提供属性存取与加密写出能力。
 */
public class NettySession implements CPSession {

    private static final Logger log = LoggerFactory.getLogger(NettySession.class);
    private static final int NONCE_LENGTH = 12;
    private static final byte[] ZERO_NONCE = new byte[NONCE_LENGTH];

    // netty channel 上下文，用于进行数据的读写
    private final ChannelHandlerContext context;
    // 会话属性，使用 ConcurrentHashMap 保证多线程读写安全
    private final Map<String,Object> attributes = new ConcurrentHashMap<>();

    public NettySession(ChannelHandlerContext context) {
        this.context = context;
        init();
    }

    private void init() {
        // 初始化基础属性
        // AES 密钥（Base64 编码）
        attributes.put(ConnectionAttributes.ENCRYPTION_KEY, "");
        // 是否已完成加密握手
        attributes.put(ConnectionAttributes.ENCRYPTION_STATE, false);
        // 会话唯一 id
        attributes.put(ConnectionAttributes.PACKAGE_SESSION_ID, IdUtil.generateId());
        // 最近接收的包序列
        attributes.put(ConnectionAttributes.PACKAGE_ID, 0);
        // 即将发送的本地包序列
        attributes.put(ConnectionAttributes.LOCAL_PACKAGE_ID, 0);
    }

    @Override
    public void write(String msg, boolean encrypted) {
        byte[] aad = buildAad();

        if (encrypted) {
            writeEncrypted(msg, aad);
        } else {
            writePlain(msg, aad);
        }

    }

    /**
     * 构建 AAD（Additional Authenticated Data）。
     */
    private byte[] buildAad() {
        Integer localPackageId = getAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, Integer.class);
        if (localPackageId == null) {
            localPackageId = 0;
        }
        Long sessionId = getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
        if (sessionId == null) {
            sessionId = 0L;
        }
        AeadAad aeadAad = new AeadAad(localPackageId, sessionId, System.currentTimeMillis());
        // 包序列自增
        setAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, localPackageId + 1);
        return aeadAad.encode();
    }

    private void writeEncrypted(String msg, byte[] aad) {
        AESData aesData;
        try {
            aesData = AESUtil.encryptWithAAD(
                    msg,
                    aad,
                    Base64.decode(getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, String.class))
            );
        } catch (Exception e) {
            Long sessionIdForLog = getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
            log.error("unexpected error: AES encrypt failed, sessionId={}", sessionIdForLog, e);
            DisconnectSupport.markDisconnect(context.channel(), this, "encrypt_failed", e);
            close();
            return;
        }
        byte[] data = new byte[aesData.ciphertext().length + aad.length + aesData.nonce().length];
        // 填入 nonce
        System.arraycopy(aesData.nonce(), 0, data, 0, aesData.nonce().length);
        // 填入 aad
        System.arraycopy(aad, 0, data, aesData.nonce().length, aad.length);
        // 填入密文
        System.arraycopy(aesData.ciphertext(), 0, data,
                aesData.nonce().length + aad.length, aesData.ciphertext().length);
        context.writeAndFlush(data);
    }

    private void writePlain(String msg, byte[] aad) {
        byte[] bytes = msg.getBytes();
        byte[] data = new byte[bytes.length + aad.length + NONCE_LENGTH];
        // 填入空 nonce（12 字节全 0）
        System.arraycopy(ZERO_NONCE, 0, data, 0, NONCE_LENGTH);
        // 填入 aad
        System.arraycopy(aad, 0, data, NONCE_LENGTH, aad.length);
        // 填入明文
        System.arraycopy(bytes, 0, data, 32, bytes.length);
        context.writeAndFlush(data);
    }

    @Override
    public <T> T getAttributeValue(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (!type.isInstance(value)) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void close() {
        context.close();
    }
}
