package team.carrypigeon.backend.connectionpool.attribute;

import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.bo.domain.CPSession;

public class ConnectionAttributes {
    public static final AttributeKey<CPSession> SESSIONS = AttributeKey.valueOf("SESSIONS");
    public static final String ENCRYPTION_KEY = "EncryptionKey";
    public static final String ENCRYPTION_STATE = "EncryptionState";
    public static final String PACKAGE_SESSION_ID = "SessionId";
    public static final String PACKAGE_ID = "PackageId";
    public static final String LOCAL_PACKAGE_ID = "LocalPackageId";
    public static final String PACKAGE_TIMESTAMP = "PackageTimestamp";
}
