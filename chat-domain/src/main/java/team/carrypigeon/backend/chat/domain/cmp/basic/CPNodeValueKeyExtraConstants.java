package team.carrypigeon.backend.chat.domain.cmp.basic;

/**
 * LiteFlow 节点在自定义上下文（{@link team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext}，继承自 DefaultContext）
 * 中读写数据时使用的补充型 key 常量。
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

    // -------- 频道成员扩展 --------

    /** Long: 目标成员 uid（用于部分需要同时校验自身成员身份和查询目标成员的链路） */
    public static final String TARGET_MEMBER_UID = "TargetMember_Uid";

    // -------- 通用辅助对象 --------

    /** PageInfo: 分页信息对象 */
    public static final String PAGE_INFO = "PageInfo";
}
