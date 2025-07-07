package team.carrypigeon.backend.connectionpool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;

@AllArgsConstructor
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler<String> {

    private CPControllerDispatcher cpControllerDispatcher;
    private ObjectMapper objectMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            ctx.writeAndFlush(objectMapper.writeValueAsString(cpControllerDispatcher.process(msg)));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}