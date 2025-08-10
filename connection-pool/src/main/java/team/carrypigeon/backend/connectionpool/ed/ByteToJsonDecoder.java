package team.carrypigeon.backend.connectionpool.ed;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ByteToJsonDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // 如果连长度都没接受到就停止读取
        if (byteBuf.readableBytes()<4){
            return;
        }
        int length = byteBuf.readInt();
        if (byteBuf.readableBytes() < length){
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        list.add(new String(bytes));
    }
}
