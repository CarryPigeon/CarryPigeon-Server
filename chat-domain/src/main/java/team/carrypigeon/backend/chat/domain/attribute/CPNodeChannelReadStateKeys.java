package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * 已读状态 ChannelReadStateInfo_* 相关上下文 key。
 */
public final class CPNodeChannelReadStateKeys {

    /** {@code CPChannelReadState}: 已读状态实体。 */
    public static final CPKey<CPChannelReadState> CHANNEL_READ_STATE_INFO = CPKey.of("ChannelReadStateInfo", CPChannelReadState.class);
    /** {@code Long}: 记录 id。 */
    public static final CPKey<Long> CHANNEL_READ_STATE_INFO_ID = CPKey.of("ChannelReadStateInfo_Id", Long.class);
    /** {@code Long}: 用户 uid。 */
    public static final CPKey<Long> CHANNEL_READ_STATE_INFO_UID = CPKey.of("ChannelReadStateInfo_Uid", Long.class);
    /** {@code Long}: 频道 cid。 */
    public static final CPKey<Long> CHANNEL_READ_STATE_INFO_CID = CPKey.of("ChannelReadStateInfo_Cid", Long.class);
    /** {@code Long}: 最后已读消息 id（mid）。 */
    public static final CPKey<Long> CHANNEL_READ_STATE_INFO_LAST_READ_MID = CPKey.of("ChannelReadStateInfo_LastReadMid", Long.class);
    /** {@code Long}: 最后已读时间（毫秒时间戳）。 */
    public static final CPKey<Long> CHANNEL_READ_STATE_INFO_LAST_READ_TIME = CPKey.of("ChannelReadStateInfo_LastReadTime", Long.class);

    private CPNodeChannelReadStateKeys() {
    }
}
