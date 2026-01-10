package team.carrypigeon.backend.connection.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.disconnect.DisconnectSupport;

import java.util.List;

/**
 * 简单的长度前缀解码器：
 * <pre>
 *  - 前 2 字节为无符号 short，表示后续 payload 长度；
 *  - 后面紧跟 payload 字节。
 * </pre>
 * 做了以下防御性处理：
 *  - 使用 mark/reset 保证半包不破坏 readerIndex；
 *  - 对 length 做上限校验，防止异常大包攻击。
 */
public class NettyDecoder extends ByteToMessageDecoder {

    private static final int MAX_FRAME_LENGTH = 64 * 1024; // 64 KB

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 至少需要 2 字节长度
        if (in.readableBytes() < 2) {
            return;
        }
        in.markReaderIndex();

        int length = in.readUnsignedShort();
        if (length <= 0 || length > MAX_FRAME_LENGTH) {
            CPSession session = ctx.channel().attr(ConnectionAttributes.SESSIONS).get();
            DisconnectSupport.markDisconnect(
                    ctx.channel(),
                    session,
                    "frame_length_illegal",
                    "FrameLengthIllegal",
                    "length=" + length + ", max=" + MAX_FRAME_LENGTH
            );
            // 直接关闭连接，避免继续读取异常数据
            ctx.close();
            return;
        }

        if (in.readableBytes() < length) {
            // 半包，恢复 readerIndex，等待下次读取
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        out.add(bytes);
    }
}
