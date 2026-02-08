package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.util.Set;

/**
 * 频道实体 ChannelInfo_* 相关 key。
 */
public final class CPNodeChannelKeys {

    /** {@code CPChannel}: 频道实体。 */
    public static final CPKey<CPChannel> CHANNEL_INFO = CPKey.of("ChannelInfo", CPChannel.class);
    /** {@code Long}: 频道 id。 */
    public static final CPKey<Long> CHANNEL_INFO_ID = CPKey.of("ChannelInfo_Id", Long.class);
    /** {@code String}: 频道名。 */
    public static final CPKey<String> CHANNEL_INFO_NAME = CPKey.of("ChannelInfo_Name", String.class);
    /** {@code Long}: 频道所有者 uid。 */
    public static final CPKey<Long> CHANNEL_INFO_OWNER = CPKey.of("ChannelInfo_Owner", Long.class);
    /** {@code String}: 频道简介。 */
    public static final CPKey<String> CHANNEL_INFO_BRIEF = CPKey.of("ChannelInfo_Brief", String.class);
    /** {@code Long}: 频道头像文件 id。 */
    public static final CPKey<Long> CHANNEL_INFO_AVATAR = CPKey.of("ChannelInfo_Avatar", Long.class);
    /** {@code Long}: 创建时间（毫秒时间戳）。 */
    public static final CPKey<Long> CHANNEL_INFO_CREATE_TIME = CPKey.of("ChannelInfo_CreateTime", Long.class);
    /** {@code Set<CPChannel>}: 频道列表。 */
    public static final CPKey<Set> CHANNEL_INFO_LIST = CPKey.of("channels", Set.class);

    private CPNodeChannelKeys() {
    }
}
