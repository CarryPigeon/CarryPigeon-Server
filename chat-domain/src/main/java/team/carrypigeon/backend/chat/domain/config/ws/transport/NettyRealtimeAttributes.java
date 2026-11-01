package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;

public class NettyRealtimeAttributes implements RealtimeAttributes {
    /** 会话 ID。 */
    private static final AttributeKey<String> SESSION_ID_KEY =
        AttributeKey.valueOf("cp.realtime.session-id");

    /** 已认证主体。 */
    private static final AttributeKey<Long> AUTHENTICATED_PRINCIPAL_KEY =
        AttributeKey.valueOf("cp.realtime.authenticated-principal");

    private final Channel channel;

    public NettyRealtimeAttributes(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void setSessionId(String sessionId) {
        channel.attr(SESSION_ID_KEY).set(sessionId);
    }

    @Override
    public String getSessionId() {
        return channel.attr(SESSION_ID_KEY).get();
    }

    @Override
    public void setAccountId(Long accountId) {
        channel.attr(AUTHENTICATED_PRINCIPAL_KEY).set(accountId);

    }

    @Override
    public Long getAccountId() {
        return channel.attr(AUTHENTICATED_PRINCIPAL_KEY).get();
    }
}
