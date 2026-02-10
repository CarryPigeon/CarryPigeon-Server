package team.carrypigeon.backend.chat.domain.attribute;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * 消息 MessageInfo_* / MessageList_* 相关 key。
 */
public final class CPNodeMessageKeys {

    /** {@code CPMessage}: 消息实体。 */
    public static final CPKey<CPMessage> MESSAGE_INFO = CPKey.of("MessageInfo", CPMessage.class);
    /** {@code Long}: 消息 id。 */
    public static final CPKey<Long> MESSAGE_INFO_ID = CPKey.of("MessageInfo_Id", Long.class);
    /** Long: 发送用户 id */
    public static final CPKey<Long> MESSAGE_INFO_UID = CPKey.of("MessageInfo_Uid", Long.class);
    /** Long: 所属频道 id */
    public static final CPKey<Long> MESSAGE_INFO_CID = CPKey.of("MessageInfo_Cid", Long.class);
    /** {@code String}: 消息 domain（例如 Core:Text）。 */
    public static final CPKey<String> MESSAGE_INFO_DOMAIN = CPKey.of("MessageInfo_Domain", String.class);
    /** {@code String}: 消息 domain version（例如 1.0.0）。 */
    public static final CPKey<String> MESSAGE_INFO_DOMAIN_VERSION = CPKey.of("MessageInfo_DomainVersion", String.class);
    /** {@code Long}: 回复目标消息 id（mid），0 表示非回复。 */
    public static final CPKey<Long> MESSAGE_INFO_REPLY_TO_MID = CPKey.of("MessageInfo_ReplyToMid", Long.class);
    /** {@code JsonNode}: 消息 data（按 domain 定义其结构）。 */
    public static final CPKey<JsonNode> MESSAGE_INFO_DATA = CPKey.of("MessageInfo_Data", JsonNode.class);

    /** {@code CPMessage[]}: 消息列表。 */
    public static final CPKey<CPMessage[]> MESSAGE_LIST = CPKey.of("Messages", CPMessage[].class);
    /** {@code Long}: 拉取游标消息 id（mid，exclusive）。 */
    public static final CPKey<Long> MESSAGE_LIST_CURSOR_MID = CPKey.of("MessageList_CursorMid", Long.class);
    /** {@code Integer}: 拉取数量。 */
    public static final CPKey<Integer> MESSAGE_LIST_COUNT = CPKey.of("MessageList_Count", Integer.class);

    /** {@code Long}: 未读统计起始消息 id（mid，exclusive）。 */
    public static final CPKey<Long> MESSAGE_UNREAD_START_MID = CPKey.of("MessageUnread_StartMid", Long.class);
    /** {@code Long}: 未读数量。 */
    public static final CPKey<Long> MESSAGE_UNREAD_COUNT = CPKey.of("MessageUnread_Count", Long.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeMessageKeys() {
    }
}
