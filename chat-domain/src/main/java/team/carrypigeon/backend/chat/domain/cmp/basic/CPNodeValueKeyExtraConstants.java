package team.carrypigeon.backend.chat.domain.cmp.basic;

/**
 * Extra key constants used by LiteFlow nodes when reading/writing data
 * in {@link com.yomahub.liteflow.slot.DefaultContext}.
 * <p>
 * These keys complement {@link CPNodeValueKeyBasicConstants} for:
 * <ul>
 *     <li>Message parsing helper objects</li>
 *     <li>UserToken related context data</li>
 *     <li>Email verification related context data</li>
 *     <li>Generic helper objects such as PageInfo</li>
 * </ul>
 */
public final class CPNodeValueKeyExtraConstants {

    private CPNodeValueKeyExtraConstants() {
    }

    // -------- MessageData --------

    /** CPMessageData: 解析后的消息业务对象 */
    public static final String MESSAGE_DATA = "MessageData";

    // -------- UserToken --------

    /** CPUserToken: 用户登录 Token 实体 */
    public static final String USER_TOKEN = "UserToken";

    /** Long: 用户 Token 主键 id */
    public static final String USER_TOKEN_ID = "UserToken_Id";

    /** String: Token 字符串值 */
    public static final String USER_TOKEN_TOKEN = "UserToken_Token";

    /** Long: Token 过期时间戳（毫秒） */
    public static final String USER_TOKEN_EXPIRED_TIME = "UserToken_ExpiredTime";

    // -------- 邮箱验证码相关 --------

    /** String: 用户邮箱地址 */
    public static final String EMAIL = "Email";

    /** Long: 邮箱验证码值 */
    public static final String EMAIL_CODE = "Email_Code";

    // -------- ChannelApplicationInfo 扩展 --------

    /** Long: 频道申请主键 id */
    public static final String CHANNEL_APPLICATION_INFO_ID = "ChannelApplicationInfo_Id";

    // -------- 通用辅助对象 --------

    /** PageInfo: 分页信息对象 */
    public static final String PAGE_INFO = "PageInfo";
}

