package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 消息 MessageInfo_* / MessageList_* 相关 key。
 */
public final class CPNodeMessageKeys {

    public static final String MESSAGE_INFO = "MessageInfo";
    public static final String MESSAGE_INFO_ID = "MessageInfo_Id";
    /** Long: 发送用户 id */
    public static final String MESSAGE_INFO_UID = "MessageInfo_Uid";
    /** Long: 所属频道 id */
    public static final String MESSAGE_INFO_CID = "MessageInfo_Cid";
    public static final String MESSAGE_INFO_DOMAIN = "MessageInfo_Domain";
    public static final String MESSAGE_INFO_DATA = "MessageInfo_Data";

    public static final String MESSAGE_LIST = "Messages";
    public static final String MESSAGE_LIST_START_TIME = "MessageList_StartTime";
    public static final String MESSAGE_LIST_COUNT = "MessageList_Count";

    public static final String MESSAGE_UNREAD_START_TIME = "MessageUnread_StartTime";
    public static final String MESSAGE_UNREAD_COUNT = "MessageUnread_Count";

    private CPNodeMessageKeys() {
    }
}
