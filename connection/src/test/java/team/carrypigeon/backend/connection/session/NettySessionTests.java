package team.carrypigeon.backend.connection.session;

import cn.hutool.core.codec.Base64;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.protocol.aad.AeadAad;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NettySessionTests {

    @Test
    void writePlain_shouldWriteNonceAadAndPlainPayload() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        NettySession session = new NettySession(ctx);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(ctx.writeAndFlush(captor.capture())).thenReturn(null);

        long sessionId = session.getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class);
        assertNotNull(sessionId);

        session.write("hello", false);
        verify(ctx, times(1)).writeAndFlush(any(byte[].class));

        byte[] frame = captor.getValue();
        assertNotNull(frame);
        assertEquals(32 + "hello".getBytes().length, frame.length);

        // nonce 12 bytes all-zero for plaintext
        for (int i = 0; i < 12; i++) {
            assertEquals(0, frame[i]);
        }

        byte[] aadBytes = new byte[AeadAad.LENGTH];
        System.arraycopy(frame, 12, aadBytes, 0, AeadAad.LENGTH);
        AeadAad aad = AeadAad.decode(aadBytes);
        assertEquals(0, aad.getPackageId());
        assertEquals(sessionId, aad.getSessionId());

        Integer localPackageId = session.getAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, Integer.class);
        assertEquals(1, localPackageId);

        byte[] plain = new byte[frame.length - 32];
        System.arraycopy(frame, 32, plain, 0, plain.length);
        assertEquals("hello", new String(plain));
    }

    @Test
    void writeEncrypted_shouldWriteEncryptPayloadThatCanBeDecrypted() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        NettySession session = new NettySession(ctx);

        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        session.setAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, Base64.encode(keyBytes));

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(ctx.writeAndFlush(captor.capture())).thenReturn(null);

        session.write("hello", true);
        verify(ctx, times(1)).writeAndFlush(any(byte[].class));

        byte[] frame = captor.getValue();
        assertNotNull(frame);
        assertTrue(frame.length > 32);

        byte[] nonce = new byte[12];
        byte[] aadBytes = new byte[AeadAad.LENGTH];
        byte[] ciphertext = new byte[frame.length - 32];
        System.arraycopy(frame, 0, nonce, 0, 12);
        System.arraycopy(frame, 12, aadBytes, 0, AeadAad.LENGTH);
        System.arraycopy(frame, 32, ciphertext, 0, ciphertext.length);

        AeadAad aad = AeadAad.decode(aadBytes);
        assertEquals(0, aad.getPackageId());

        String decrypted = AESUtil.decryptWithAAD(ciphertext, nonce, aadBytes, keyBytes);
        assertEquals("hello", decrypted);
    }

    @Test
    void writeEncrypted_withoutKey_shouldCloseAndNotWrite() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        NettySession session = new NettySession(ctx);

        session.write("hello", true);

        verify(ctx, never()).writeAndFlush(any(byte[].class));
        verify(ctx, times(1)).close();
    }

    @Test
    void writePlain_whenAadAttributesMissing_shouldDefaultAndIncrementPackageId() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        NettySession session = new NettySession(ctx);

        session.setAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, "bad-type");
        session.setAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, "bad-type");

        when(ctx.writeAndFlush(any(byte[].class))).thenReturn(null);
        session.write("hello", false);

        assertEquals(1, session.getAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, Integer.class));
        verify(ctx, times(1)).writeAndFlush(any(byte[].class));
    }
}
