package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * 用户 Token 相关上下文 key 封装（UserTokenInfo_*）。
 */
public final class CPNodeUserTokenKeys {

    /** {@code CPUserToken}: token 实体。 */
    public static final CPKey<CPUserToken> USER_TOKEN_INFO = CPKey.of("UserTokenInfo", CPUserToken.class);
    /** {@code Long}: token 记录 id。 */
    public static final CPKey<Long> USER_TOKEN_INFO_ID = CPKey.of("UserTokenInfo_Id", Long.class);
    /** Long: 关联用户 id */
    public static final CPKey<Long> USER_TOKEN_INFO_UID = CPKey.of("UserTokenInfo_Uid", Long.class);
    /** {@code String}: token 字符串（注意脱敏日志）。 */
    public static final CPKey<String> USER_TOKEN_INFO_TOKEN = CPKey.of("UserTokenInfo_Token", String.class);
    /** {@code Long}: 过期时间（毫秒时间戳）。 */
    public static final CPKey<Long> USER_TOKEN_INFO_EXPIRED_TIME = CPKey.of("UserTokenInfo_ExpiredTime", Long.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeUserTokenKeys() {
    }
}
