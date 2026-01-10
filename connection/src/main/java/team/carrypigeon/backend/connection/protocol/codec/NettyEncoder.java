package team.carrypigeon.backend.connection.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, byte[] bytes, ByteBuf byteBuf) {
        byteBuf.writeShort(bytes.length);
        byteBuf.writeBytes(bytes);
    }
}
