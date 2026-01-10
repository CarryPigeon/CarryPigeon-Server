package team.carrypigeon.backend.connection.disconnect;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;

public final class DisconnectSupport {

    private DisconnectSupport() {
    }

    private static final AttributeKey<Boolean> DISCONNECT_LOGGED = AttributeKey.valueOf("CP_DISCONNECT_LOGGED");
    private static final AttributeKey<String> DISCONNECT_REASON = AttributeKey.valueOf("CP_DISCONNECT_REASON");
    private static final AttributeKey<String> DISCONNECT_CAUSE_TYPE = AttributeKey.valueOf("CP_DISCONNECT_CAUSE_TYPE");
    private static final AttributeKey<String> DISCONNECT_CAUSE_MESSAGE = AttributeKey.valueOf("CP_DISCONNECT_CAUSE_MESSAGE");

    public static boolean isDisconnectLogged(Channel channel) {
        return channel != null && Boolean.TRUE.equals(channel.attr(DISCONNECT_LOGGED).get());
    }

    public static void markDisconnectLogged(Channel channel) {
        if (channel == null) {
            return;
        }
        channel.attr(DISCONNECT_LOGGED).set(true);
    }

    public static DisconnectInfo resolveDisconnectInfo(Channel channel, CPSession session) {
        String reason = channel == null ? null : channel.attr(DISCONNECT_REASON).get();
        String causeType = channel == null ? null : channel.attr(DISCONNECT_CAUSE_TYPE).get();
        String causeMessage = channel == null ? null : channel.attr(DISCONNECT_CAUSE_MESSAGE).get();

        if (reason == null && session != null) {
            reason = session.getAttributeValue(ConnectionAttributes.DISCONNECT_REASON, String.class);
        }
        if (causeType == null && session != null) {
            causeType = session.getAttributeValue(ConnectionAttributes.DISCONNECT_CAUSE_TYPE, String.class);
        }
        if (causeMessage == null && session != null) {
            causeMessage = session.getAttributeValue(ConnectionAttributes.DISCONNECT_CAUSE_MESSAGE, String.class);
        }

        return new DisconnectInfo(reason, causeType, causeMessage);
    }

    public static void markDisconnect(Channel channel, CPSession session, String reason, Throwable cause) {
        Throwable rootCause = rootCause(cause);
        String causeType = rootCause == null ? null : rootCause.getClass().getName();
        String causeMessage = rootCause == null ? null : rootCause.getMessage();
        markDisconnect(channel, session, reason, causeType, causeMessage);
    }

    public static void markDisconnect(Channel channel, CPSession session, String reason, String causeType, String causeMessage) {
        if (channel != null) {
            setIfAbsent(channel, DISCONNECT_REASON, reason);
            setIfAbsent(channel, DISCONNECT_CAUSE_TYPE, causeType);
            setIfAbsent(channel, DISCONNECT_CAUSE_MESSAGE, causeMessage);
        }
        if (session != null) {
            setIfAbsent(session, ConnectionAttributes.DISCONNECT_REASON, reason);
            setIfAbsent(session, ConnectionAttributes.DISCONNECT_CAUSE_TYPE, causeType);
            setIfAbsent(session, ConnectionAttributes.DISCONNECT_CAUSE_MESSAGE, causeMessage);
        }
    }

    public static Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static void setIfAbsent(Channel channel, AttributeKey<String> key, String value) {
        if (value == null) {
            return;
        }
        if (channel.attr(key).get() == null) {
            channel.attr(key).set(value);
        }
    }

    private static void setIfAbsent(CPSession session, String key, String value) {
        if (value == null) {
            return;
        }
        if (session.getAttributeValue(key, String.class) == null) {
            session.setAttributeValue(key, value);
        }
    }

    public record DisconnectInfo(String reason, String causeType, String causeMessage) {
    }
}

