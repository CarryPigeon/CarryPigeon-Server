package team.carrypigeon.backend.connection.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NettyCodecTests {

    @Test
    void encoder_shouldPrefixLength() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyEncoder());
        byte[] payload = "hello".getBytes();

        assertTrue(channel.writeOutbound(payload));
        ByteBuf out = channel.readOutbound();
        try {
            assertNotNull(out);
            assertEquals(payload.length, out.readUnsignedShort());
            byte[] decoded = new byte[payload.length];
            out.readBytes(decoded);
            assertArrayEquals(payload, decoded);
        } finally {
            out.release();
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decoder_incompleteFrame_shouldWaitForMoreBytes() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyDecoder());

        // Only 1 byte length prefix -> should not output anything
        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{0x00})));
        assertNull(channel.readInbound());

        channel.finishAndReleaseAll();
    }

    @Test
    void decoder_halfPacket_shouldNotConsumeReaderIndex() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyDecoder());

        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(5);
        buf.writeBytes(new byte[]{1, 2}); // only 2 of 5
        assertFalse(channel.writeInbound(buf));
        assertNull(channel.readInbound());

        // Now write remaining 3 bytes; decoder should output full frame
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{3, 4, 5})));
        byte[] decoded = channel.readInbound();
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, decoded);

        channel.finishAndReleaseAll();
    }

    @Test
    void decoder_illegalLength_shouldCloseChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyDecoder());

        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(0);
        channel.writeInbound(buf);

        assertFalse(channel.isActive());
        channel.finishAndReleaseAll();
    }
}

