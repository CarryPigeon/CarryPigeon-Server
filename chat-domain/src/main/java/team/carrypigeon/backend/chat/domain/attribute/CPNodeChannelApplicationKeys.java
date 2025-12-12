package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 频道申请 ChannelApplicationInfo_* 相关 key。
 */
public final class CPNodeChannelApplicationKeys {

    public static final String CHANNEL_APPLICATION_INFO = "ChannelApplicationInfo";
    /** Long: 申请记录 id */
    // 基础 id 已由 CPNodeValueKeyExtraConstants.CHANNEL_APPLICATION_INFO_ID 定义
    public static final String CHANNEL_APPLICATION_INFO_UID = "ChannelApplicationInfo_Uid";
    public static final String CHANNEL_APPLICATION_INFO_CID = "ChannelApplicationInfo_Cid";
    public static final String CHANNEL_APPLICATION_INFO_STATE = "ChannelApplicationInfo_State";
    public static final String CHANNEL_APPLICATION_INFO_MSG = "ChannelApplicationInfo_Msg";
    public static final String CHANNEL_APPLICATION_INFO_APPLY_TIME = "ChannelApplicationInfo_ApplyTime";
    public static final String CHANNEL_APPLICATION_INFO_LIST = "applications";

    private CPNodeChannelApplicationKeys() {
    }
}
