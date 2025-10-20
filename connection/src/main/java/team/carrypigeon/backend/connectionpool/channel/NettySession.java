package team.carrypigeon.backend.connectionpool.channel;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.connectionpool.protocol.encryption.aes.AESUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class NettySession implements CPSession {

    private final ChannelHandlerContext context;
    private final Map<String,Object> attributes = new HashMap<>();

    public NettySession(ChannelHandlerContext context) {
        this.context = context;
        init();
    }

    private void init(){

    }

    @Override
    public void write(String msg) {
        try {
            context.writeAndFlush(AESUtil.encrypt(msg,AESUtil.convertStringToKey(security.getKey())));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    @Override
    public <T> Optional<T> getAttributeValue(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    @Override
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }
}
