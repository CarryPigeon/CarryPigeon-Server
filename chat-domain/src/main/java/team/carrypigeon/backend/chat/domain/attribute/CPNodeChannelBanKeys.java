package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 频道封禁 ChannelBan_* 相关 key。
 */
public final class CPNodeChannelBanKeys {

    public static final String CHANNEL_BAN_INFO = "ChannelBanInfo";
    /** Long: 禁言记录 id */
    public static final String CHANNEL_BAN_ID = "ChannelBan_Id";
    /** Long: 频道 id */
    public static final String CHANNEL_BAN_CID = "ChannelBan_Cid";
    /** Long: 被封禁用户 id（与业务中 uid 字段对应） */
    public static final String CHANNEL_BAN_TARGET_UID = "ChannelBan_TargetUid";
    /** Long: 操作管理员 id */
    public static final String CHANNEL_BAN_AID = "ChannelBan_Aid";
    public static final String CHANNEL_BAN_DURATION = "ChannelBan_Duration";
    /** Long: 创建时间（毫秒时间戳） */
    public static final String CHANNEL_BAN_CREATE_TIME = "ChannelBan_CreateTime";
    public static final String CHANNEL_BAN_ITEMS = "ChannelBanItems";

    private CPNodeChannelBanKeys() {
    }
}
