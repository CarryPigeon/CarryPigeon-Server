package team.carrypigeon.backend.chat.domain.cmp.basic;

/**
 * LiteFlow 节点在 {@link com.yomahub.liteflow.slot.DefaultContext} 中读写数据时
 * 使用的补充型 key 常量。
 * <ul>
 *   <li>消息解析辅助对象</li>
 *   <li>UserToken 相关上下文数据</li>
 *   <li>邮箱验证码相关上下文数据</li>
 *   <li>通用辅助对象（如 PageInfo）</li>
 * </ul>
 */
public final class CPNodeValueKeyExtraConstants {

    private CPNodeValueKeyExtraConstants() {
    }

    // -------- MessageData --------

    /** CPMessageData: 解析后的消息业务对象 */
    public static final String MESSAGE_DATA = "MessageData";


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
