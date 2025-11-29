package team.carrypigeon.backend.chat.domain.cmp.basic;

/**
 * LiteFlow {@link com.yomahub.liteflow.slot.DefaultContext} 中使用的公共 key 常量。
 * <p>
 * 命名规范：
 * <ul>
 *   <li>实体对象：UserInfo / ChannelInfo / ChannelMemberInfo / ChannelApplicationInfo / MessageInfo / ChannelBanInfo / FileInfo</li>
 *   <li>实体字段：{EntityName}_FieldName，例如 UserInfo_Id、ChannelMemberInfo_Uid</li>
 *   <li>集合：{EntityName}List，例如 ChannelMemberInfoList</li>
 *   <li>其他辅助字段（如 SessionId / Notifier_*）使用语义化命名。</li>
 * </ul>
 */
public final class CPNodeValueKeyBasicConstants {

    private CPNodeValueKeyBasicConstants() {
    }

    // -------- Session / Checker --------

    /** CPSession: 当前会话对象 */
    public static final String SESSION = "session";

    /** Long: 当前登录用户的 id */
    public static final String SESSION_ID = "SessionId";

    /** CheckResult: 最近一次软失败检查结果 */
    public static final String CHECK_RESULT = "CheckResult";

    // -------- 通用响应 --------

    /** CPResponse: 最终返回给客户端的响应 */
    public static final String RESPONSE = "response";

    // -------- UserInfo --------

    /** CPUser: 当前上下文中的用户实体 */
    public static final String USER_INFO = "UserInfo";

    /** Long: 用户 id */
    public static final String USER_INFO_ID = "UserInfo_Id";

    /** String: 用户名 */
    public static final String USER_INFO_USER_NAME = "UserInfo_UserName";

    /** String: 用户邮箱 */
    public static final String USER_INFO_EMAIL = "UserInfo_Email";

    /** Integer: 用户性别（枚举值） */
    public static final String USER_INFO_SEX = "UserInfo_Sex";

    /** Long: 用户生日时间戳（毫秒） */
    public static final String USER_INFO_BIRTHDAY = "UserInfo_Birthday";

    /** Long: 用户注册时间戳（毫秒） */
    public static final String USER_INFO_REGISTER_TIME = "UserInfo_RegisterTime";

    /** String: 用户简介 */
    public static final String USER_INFO_BRIEF = "UserInfo_Brief";

    /** Long: 用户头像 id */
    public static final String USER_INFO_AVATAR = "UserInfo_Avatar";

    // -------- ChannelInfo --------

    /** CPChannel: 当前上下文中的频道实体 */
    public static final String CHANNEL_INFO = "ChannelInfo";

    /** Long: 频道 id */
    public static final String CHANNEL_INFO_ID = "ChannelInfo_Id";

    /** String: 频道名称 */
    public static final String CHANNEL_INFO_NAME = "ChannelInfo_Name";

    /** Long: 频道所有者用户 id */
    public static final String CHANNEL_INFO_OWNER = "ChannelInfo_Owner";

    /** String: 频道简介 */
    public static final String CHANNEL_INFO_BRIEF = "ChannelInfo_Brief";

    /** Long: 频道头像 id */
    public static final String CHANNEL_INFO_AVATAR = "ChannelInfo_Avatar";

    /** Long: 频道创建时间戳（毫秒） */
    public static final String CHANNEL_INFO_CREATE_TIME = "ChannelInfo_CreateTime";

    /** CPChannel[] 或集合：频道集合 */
    public static final String CHANNEL_INFO_LIST = "channels";

    // -------- ChannelMemberInfo --------

    /** CPChannelMember: 当前上下文中的频道成员实体 */
    public static final String CHANNEL_MEMBER_INFO = "ChannelMemberInfo";

    /** Long: 频道成员 id */
    public static final String CHANNEL_MEMBER_INFO_ID = "ChannelMemberInfo_Id";

    /** Long: 频道成员对应的用户 id */
    public static final String CHANNEL_MEMBER_INFO_UID = "ChannelMemberInfo_Uid";

    /** Long: 频道成员所在频道 id */
    public static final String CHANNEL_MEMBER_INFO_CID = "ChannelMemberInfo_Cid";

    /** String: 频道成员备注名称 */
    public static final String CHANNEL_MEMBER_INFO_NAME = "ChannelMemberInfo_Name";

    /** Integer: 频道成员权限（枚举值） */
    public static final String CHANNEL_MEMBER_INFO_AUTHORITY = "ChannelMemberInfo_Authority";

    /** String: 频道申请时附带的消息 */
    public static final String CHANNEL_MEMBER_INFO_MSG = "ChannelMemberInfo_Msg";

    /** CPChannelMember[] 或集合：频道成员集合 */
    public static final String CHANNEL_MEMBER_INFO_LIST = "members";

    // -------- ChannelApplicationInfo --------

    /** CPChannelApplication: 频道申请实体 */
    public static final String CHANNEL_APPLICATION_INFO = "ChannelApplicationInfo";

    /** Long: 申请所属频道 id */
    public static final String CHANNEL_APPLICATION_INFO_CID = "ChannelApplicationInfo_Cid";

    /** Integer: 申请状态（枚举值） */
    public static final String CHANNEL_APPLICATION_INFO_STATE = "ChannelApplicationInfo_State";

    /** CPChannelApplication[] 或集合：频道申请集合 */
    public static final String CHANNEL_APPLICATION_INFO_LIST = "applications";

    // -------- ChannelBanInfo --------

    /** CPChannelBan: 频道封禁实体 */
    public static final String CHANNEL_BAN_INFO = "ChannelBanInfo";

    /** Long: 被封禁目标用户 id */
    public static final String CHANNEL_BAN_TARGET_UID = "ChannelBan_TargetUid";

    /** Integer: 封禁时长（秒） */
    public static final String CHANNEL_BAN_DURATION = "ChannelBan_Duration";

    /** List<CPChannelListBanResultItem>: 封禁列表结果 */
    public static final String CHANNEL_BAN_ITEMS = "ChannelBanItems";

    // -------- MessageInfo --------

    /** CPMessage: 消息实体 */
    public static final String MESSAGE_INFO = "MessageInfo";

    /** Long: 消息 id */
    public static final String MESSAGE_INFO_ID = "MessageInfo_Id";

    /** String: 消息领域，例如 Core:Text */
    public static final String MESSAGE_INFO_DOMAIN = "MessageInfo_Domain";

    /** JsonNode: 消息原始数据 */
    public static final String MESSAGE_INFO_DATA = "MessageInfo_Data";

    /** CPMessage[]: 消息列表 */
    public static final String MESSAGE_LIST = "Messages";

    /** Long: 拉取消息列表的起始时间（毫秒） */
    public static final String MESSAGE_LIST_START_TIME = "MessageList_StartTime";

    /** Integer: 拉取消息数量 */
    public static final String MESSAGE_LIST_COUNT = "MessageList_Count";

    /** Long: 未读统计起始时间（毫秒） */
    public static final String MESSAGE_UNREAD_START_TIME = "MessageUnread_StartTime";

    /** Long: 未读消息数量 */
    public static final String MESSAGE_UNREAD_COUNT = "MessageUnread_Count";

    // -------- FileInfo --------

    /** String: 文件标识（例如 sha256） */
    public static final String FILE_INFO_ID = "FileInfo_Id";

    /** String: 一次性文件操作 token */
    public static final String FILE_TOKEN = "FileToken";

    // -------- 通知相关 --------

    /** Set<Long>: 需要通知的 uid 集合 */
    public static final String NOTIFIER_UIDS = "Notifier_Uids";

    /** JsonNode: 通知数据载体 */
    public static final String NOTIFIER_DATA = "Notifier_Data";

}
