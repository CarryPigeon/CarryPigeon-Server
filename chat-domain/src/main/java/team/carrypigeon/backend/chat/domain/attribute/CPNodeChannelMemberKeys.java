package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 频道成员 ChannelMemberInfo_* 相关 key。
 */
public final class CPNodeChannelMemberKeys {

    public static final String CHANNEL_MEMBER_INFO = "ChannelMemberInfo";
    public static final String CHANNEL_MEMBER_INFO_ID = "ChannelMemberInfo_Id";
    public static final String CHANNEL_MEMBER_INFO_UID = "ChannelMemberInfo_Uid";
    public static final String CHANNEL_MEMBER_INFO_CID = "ChannelMemberInfo_Cid";
    public static final String CHANNEL_MEMBER_INFO_NAME = "ChannelMemberInfo_Name";
    public static final String CHANNEL_MEMBER_INFO_AUTHORITY = "ChannelMemberInfo_Authority";
    public static final String CHANNEL_MEMBER_INFO_MSG = "ChannelMemberInfo_Msg";
    /** LocalDateTime: 成员加入时间 */
    public static final String CHANNEL_MEMBER_INFO_JOIN_TIME = "ChannelMemberInfo_JoinTime";
    public static final String CHANNEL_MEMBER_INFO_LIST = "members";

    private CPNodeChannelMemberKeys() {
    }
}
