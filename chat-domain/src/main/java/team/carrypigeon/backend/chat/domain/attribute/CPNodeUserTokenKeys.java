package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 用户 Token 相关上下文 key 封装（UserTokenInfo_*）。
 */
public final class CPNodeUserTokenKeys {

    public static final String USER_TOKEN_INFO = "UserTokenInfo";
    public static final String USER_TOKEN_INFO_ID = "UserTokenInfo_Id";
    /** Long: 关联用户 id */
    public static final String USER_TOKEN_INFO_UID = "UserTokenInfo_Uid";
    public static final String USER_TOKEN_INFO_TOKEN = "UserTokenInfo_Token";
    public static final String USER_TOKEN_INFO_EXPIRED_TIME = "UserTokenInfo_ExpiredTime";

    private CPNodeUserTokenKeys() {
    }
}
