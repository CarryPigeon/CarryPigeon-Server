package team.carrypigeon.backend.connectionpool;

import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.connectionpool.security.CPClientSecurity;

public class ConnectionConstant {
    public static final AttributeKey<CPClientSecurity> SECURITY_STATE = AttributeKey.valueOf("SecurityState");
    public static final AttributeKey<CPChannel> CHANNEL = AttributeKey.valueOf("Channel");
}
