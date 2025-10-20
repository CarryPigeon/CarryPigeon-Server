package team.carrypigeon.backend.connectionpool.attribute;

import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.bo.domain.CPSession;

public class ConnectionAttributes {
    public static final AttributeKey<CPSession> SESSIONS = AttributeKey.valueOf("SESSIONS");
    public static final String ENCRYPTION_KEY = "encryption";
}
