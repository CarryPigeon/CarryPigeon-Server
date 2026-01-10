package team.carrypigeon.backend.connection.handler;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.DefaultAttributeMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.api.bo.connection.CPConnectionAttributes;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.protocol.aad.AeadAad;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESData;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;
import team.carrypigeon.backend.connection.security.CPAESKeyPack;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConnectionHandlerTests {

    private static final class TestableConnectionHandler extends ConnectionHandler {
        private TestableConnectionHandler(CPControllerDispatcher dispatcher, ObjectMapper mapper, PrivateKey privateKey) {
            super(dispatcher, mapper, privateKey);
        }

        private void invokeChannelRead0(ChannelHandlerContext ctx, byte[] msg) {
            super.channelRead0(ctx, msg);
        }
    }

    private static class RecordingSession implements CPSession {
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private String lastWriteMsg;
        private boolean lastWriteEncrypted;
        private int writeCount;
        private boolean closed;

        @Override
        public void write(String msg, boolean encrypted) {
            this.lastWriteMsg = msg;
            this.lastWriteEncrypted = encrypted;
            this.writeCount++;
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
            this.closed = true;
        }
    }

    @Test
    void channelRead_invalidPacket_shouldReturn() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.writeInbound(new byte[31]);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_withoutSession_shouldReturn() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.writeInbound(new byte[32]);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_handshake_shouldSetEncryptionKeyAndWriteHandshakeNotification() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        PrivateKey serverPrivateKey = pair.getPrivate();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, serverPrivateKey);

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, false);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 123L);

        byte[] aesKeyBytes = AESUtil.generateKey().getEncoded();
        String aesKeyBase64 = java.util.Base64.getEncoder().encodeToString(aesKeyBytes);
        byte[] encryptedKeyBytes = ECCUtil.encrypt(aesKeyBase64, pair.getPublic());
        String encryptedKeyBase64 = java.util.Base64.getEncoder().encodeToString(encryptedKeyBytes);

        CPAESKeyPack keyPack = new CPAESKeyPack(1L, 123L, encryptedKeyBase64);
        byte[] payload = objectMapper.writeValueAsBytes(keyPack);

        byte[] packet = new byte[32 + payload.length];
        System.arraycopy(payload, 0, packet, 32, payload.length);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertEquals(aesKeyBase64, session.getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, String.class));
        assertEquals(true, session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, Boolean.class));
        assertEquals(1, session.writeCount);
        assertTrue(session.lastWriteEncrypted);
        assertNotNull(session.lastWriteMsg);
        assertTrue(session.lastWriteMsg.contains("\"route\":\"handshake\""));

        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_encryptedPacket_shouldDecryptAndDispatch() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);

        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 999L);

        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(keyBytes));

        String plainPack = "{\"id\":1,\"route\":\"/core/ping\",\"data\":{}}";
        byte[] aadBytes = new AeadAad(1, 999L, System.currentTimeMillis()).encode();
        AESData encrypted = AESUtil.encryptWithAAD(plainPack, aadBytes, keyBytes);

        byte[] packet = new byte[32 + encrypted.ciphertext().length];
        System.arraycopy(encrypted.nonce(), 0, packet, 0, 12);
        System.arraycopy(aadBytes, 0, packet, 12, 20);
        System.arraycopy(encrypted.ciphertext(), 0, packet, 32, encrypted.ciphertext().length);

        when(dispatcher.process(eq(plainPack), eq(session))).thenReturn(CPResponse.success());

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        verify(dispatcher, times(1)).process(eq(plainPack), eq(session));
        assertEquals(1, session.writeCount);
        assertTrue(session.lastWriteEncrypted);
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_heartbeatPacket_shouldBeIgnored() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);
        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(keyBytes));

        byte[] packet = new byte[32];
        // nonce all zeros => heartbeat; aad must still be valid to pass AAD check
        byte[] aadBytes = new AeadAad(1, 1L, System.currentTimeMillis()).encode();
        System.arraycopy(aadBytes, 0, packet, 12, 20);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        verify(dispatcher, never()).process(any(), any());
        assertEquals(0, session.writeCount);
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_handshake_illegalFormat_shouldNotWrite() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, false);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 123L);

        CPAESKeyPack keyPack = new CPAESKeyPack(0L, 123L, "a");
        byte[] payload = objectMapper.writeValueAsBytes(keyPack);

        byte[] packet = new byte[32 + payload.length];
        System.arraycopy(payload, 0, packet, 32, payload.length);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertEquals(false, session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, Boolean.class));
        assertEquals(0, session.writeCount);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_handshake_emptyAesKey_shouldNotWrite() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, false);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 123L);

        byte[] encryptedKeyBytes = ECCUtil.encrypt("", pair.getPublic());
        String encryptedKeyBase64 = java.util.Base64.getEncoder().encodeToString(encryptedKeyBytes);
        CPAESKeyPack keyPack = new CPAESKeyPack(1L, 123L, encryptedKeyBase64);
        byte[] payload = objectMapper.writeValueAsBytes(keyPack);

        byte[] packet = new byte[32 + payload.length];
        System.arraycopy(payload, 0, packet, 32, payload.length);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertEquals(false, session.getAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, Boolean.class));
        assertEquals(0, session.writeCount);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_aadMismatch_shouldCloseSession() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 5);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 10L);
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(AESUtil.generateKey().getEncoded()));

        byte[] aadBytes = new AeadAad(4, 10L, System.currentTimeMillis()).encode(); // packageId <= last => fail
        byte[] packet = new byte[32];
        System.arraycopy(aadBytes, 0, packet, 12, 20);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertTrue(session.closed);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_decryptionFailed_shouldCloseSession() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(AESUtil.generateKey().getEncoded()));

        byte[] aadBytes = new AeadAad(1, 1L, System.currentTimeMillis()).encode();
        byte[] packet = new byte[32 + 10];
        // nonce all 1 => not heartbeat
        for (int i = 0; i < 12; i++) {
            packet[i] = 1;
        }
        System.arraycopy(aadBytes, 0, packet, 12, 20);
        // ciphertext random garbage
        for (int i = 32; i < packet.length; i++) {
            packet[i] = 2;
        }

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertTrue(session.closed);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_dispatchSerializeFailed_shouldCloseSession() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);

        ConnectionHandler handler = new ConnectionHandler(dispatcher, mapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 999L);

        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(keyBytes));

        String plainPack = "{\"id\":1,\"route\":\"/core/ping\",\"data\":{}}";
        byte[] aadBytes = new AeadAad(1, 999L, System.currentTimeMillis()).encode();
        AESData encrypted = AESUtil.encryptWithAAD(plainPack, aadBytes, keyBytes);

        byte[] packet = new byte[32 + encrypted.ciphertext().length];
        System.arraycopy(encrypted.nonce(), 0, packet, 0, 12);
        System.arraycopy(aadBytes, 0, packet, 12, 20);
        System.arraycopy(encrypted.ciphertext(), 0, packet, 32, encrypted.ciphertext().length);

        when(dispatcher.process(eq(plainPack), eq(session))).thenReturn(CPResponse.success());
        when(mapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertTrue(session.closed);
        assertEquals(0, session.writeCount);
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_withSessionCleared_shouldReturn() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(null);

        channel.writeInbound(new byte[32]);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_nullMessage_shouldReturnEarly() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        TestableConnectionHandler handler = new TestableConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        assertDoesNotThrow(() -> handler.invokeChannelRead0(channel.pipeline().context(handler), null));
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_heartbeatPacket_shouldReturnWithoutDispatch() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 2L);

        byte[] aadBytes = new AeadAad(1, 2L, System.currentTimeMillis()).encode();
        byte[] packet = new byte[32];
        System.arraycopy(aadBytes, 0, packet, 12, 20);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_sessionIdMismatch_shouldCloseSession() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);

        byte[] aadBytes = new AeadAad(1, 2L, System.currentTimeMillis()).encode();
        byte[] packet = new byte[32];
        packet[0] = 1; // not heartbeat
        System.arraycopy(aadBytes, 0, packet, 12, 20);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertTrue(session.closed);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelRead_timestampTooOld_shouldCloseSession() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, objectMapper, pair.getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);

        long oldTimestamp = System.currentTimeMillis() - 180001;
        byte[] aadBytes = new AeadAad(1, 1L, oldTimestamp).encode();
        byte[] packet = new byte[32];
        packet[0] = 1; // not heartbeat
        System.arraycopy(aadBytes, 0, packet, 12, 20);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);
        channel.writeInbound(packet);

        assertTrue(session.closed);
        verify(dispatcher, never()).process(any(), any());
        channel.finishAndReleaseAll();
    }

    @Test
    void channelActiveAndInactive_shouldBindSessionAndNotifyDispatcher() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        DefaultAttributeMap attributeMap = new DefaultAttributeMap();
        Channel nettyChannel = mock(Channel.class);
        when(nettyChannel.attr(ConnectionAttributes.SESSIONS))
                .thenReturn(attributeMap.attr(ConnectionAttributes.SESSIONS));
        when(nettyChannel.remoteAddress())
                .thenReturn(new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345));

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(nettyChannel);

        assertDoesNotThrow(() -> handler.channelActive(ctx));

        CPSession session = attributeMap.attr(ConnectionAttributes.SESSIONS).get();
        assertNotNull(session);
        assertNotNull(session.getAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, String.class));
        assertEquals("127.0.0.1", session.getAttributeValue(CPConnectionAttributes.REMOTE_IP, String.class));
        assertEquals(12345, session.getAttributeValue(CPConnectionAttributes.REMOTE_PORT, Integer.class));

        assertDoesNotThrow(() -> handler.channelInactive(ctx));
        verify(dispatcher, times(1)).channelInactive(same(session));
    }

    @Test
    void exceptionCaught_connectionReset_shouldCloseChannelWithoutPropagation() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);
        session.setAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, "127.0.0.1:12345");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_IP, "127.0.0.1");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_PORT, 12345);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);

        channel.pipeline().fireExceptionCaught(new SocketException("Connection reset"));
        channel.runPendingTasks();

        assertDoesNotThrow(channel::checkException);
        assertFalse(channel.isOpen());
        channel.finishAndReleaseAll();
    }

    @Test
    void exceptionCaught_unexpectedException_shouldCloseChannelWithoutPropagation() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_STATE, true);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);
        session.setAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, "127.0.0.1:12345");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_IP, "127.0.0.1");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_PORT, 12345);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(ConnectionAttributes.SESSIONS).set(session);

        channel.pipeline().fireExceptionCaught(new RuntimeException("boom"));
        channel.runPendingTasks();

        assertDoesNotThrow(channel::checkException);
        assertFalse(channel.isOpen());
        channel.finishAndReleaseAll();
    }

    @Test
    void checkAad_invalidLength_shouldReturnFalse() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);

        assertFalse(invokeCheckAAD(handler, session, new byte[0]));
    }

    @Test
    void checkAad_negativeFields_shouldReturnFalse() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        RecordingSession session = new RecordingSession();
        session.setAttributeValue(ConnectionAttributes.PACKAGE_ID, 0);
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, 1L);

        assertFalse(invokeCheckAAD(handler, session, new AeadAad(-1, 1L, 1L).encode()));
    }

    @Test
    void dispatchToController_whenDispatcherReturnsNull_shouldNotWrite() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ConnectionHandler handler = new ConnectionHandler(dispatcher, new ObjectMapper(), ECCUtil.generateEccKeyPair().getPrivate());

        RecordingSession session = new RecordingSession();
        when(dispatcher.process(eq("p"), eq(session))).thenReturn(null);

        invokeDispatch(handler, session, "p");
        assertEquals(0, session.writeCount);
    }

    private static boolean invokeCheckAAD(ConnectionHandler handler, CPSession session, byte[] aad) throws Exception {
        Method method = ConnectionHandler.class.getDeclaredMethod("checkAAD", io.netty.channel.Channel.class, CPSession.class, byte[].class);
        method.setAccessible(true);
        EmbeddedChannel channel = new EmbeddedChannel();
        return (boolean) method.invoke(handler, channel, session, aad);
    }

    private static void invokeDispatch(ConnectionHandler handler, CPSession session, String pack) throws Exception {
        Method method = ConnectionHandler.class.getDeclaredMethod("dispatchToController", ChannelHandlerContext.class, CPSession.class, String.class);
        method.setAccessible(true);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        ChannelHandlerContext ctx = channel.pipeline().context(handler);
        method.invoke(handler, ctx, session, pack);
        channel.finishAndReleaseAll();
    }
}
