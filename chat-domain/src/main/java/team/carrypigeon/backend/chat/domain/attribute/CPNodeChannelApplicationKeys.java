package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 频道申请 ChannelApplicationInfo_* 相关 key。
 */
public final class CPNodeChannelApplicationKeys {

    /** {@code CPChannelApplication}: 频道申请实体。 */
    public static final CPKey<CPChannelApplication> CHANNEL_APPLICATION_INFO = CPKey.of("ChannelApplicationInfo", CPChannelApplication.class);
    /** {@code Long}: 申请记录 id。 */
    public static final CPKey<Long> CHANNEL_APPLICATION_INFO_ID = CPKey.of("ChannelApplicationInfo_Id", Long.class);
    /** {@code Long}: 申请用户 uid。 */
    public static final CPKey<Long> CHANNEL_APPLICATION_INFO_UID = CPKey.of("ChannelApplicationInfo_Uid", Long.class);
    /** {@code Long}: 申请频道 cid。 */
    public static final CPKey<Long> CHANNEL_APPLICATION_INFO_CID = CPKey.of("ChannelApplicationInfo_Cid", Long.class);
    /** {@code Integer}: 申请状态枚举值。 */
    public static final CPKey<Integer> CHANNEL_APPLICATION_INFO_STATE = CPKey.of("ChannelApplicationInfo_State", Integer.class);
    /** {@code String}: 申请附言/备注。 */
    public static final CPKey<String> CHANNEL_APPLICATION_INFO_MSG = CPKey.of("ChannelApplicationInfo_Msg", String.class);
    /** {@code LocalDateTime}: 申请时间。 */
    public static final CPKey<LocalDateTime> CHANNEL_APPLICATION_INFO_APPLY_TIME = CPKey.of("ChannelApplicationInfo_ApplyTime", LocalDateTime.class);
    /** {@code List<CPChannelApplication>}: 申请列表。 */
    public static final CPKey<Set> CHANNEL_APPLICATION_INFO_LIST = CPKey.of("applications", Set.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeChannelApplicationKeys() {
    }
}
