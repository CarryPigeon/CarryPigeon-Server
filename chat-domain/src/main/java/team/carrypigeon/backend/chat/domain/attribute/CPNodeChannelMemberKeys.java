package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 频道成员 ChannelMemberInfo_* 相关 key。
 */
public final class CPNodeChannelMemberKeys {

    /** {@code CPChannelMember}: 成员实体。 */
    public static final CPKey<CPChannelMember> CHANNEL_MEMBER_INFO = CPKey.of("ChannelMemberInfo", CPChannelMember.class);
    /** {@code Long}: 成员记录 id。 */
    public static final CPKey<Long> CHANNEL_MEMBER_INFO_ID = CPKey.of("ChannelMemberInfo_Id", Long.class);
    /** {@code Long}: 成员 uid。 */
    public static final CPKey<Long> CHANNEL_MEMBER_INFO_UID = CPKey.of("ChannelMemberInfo_Uid", Long.class);
    /** {@code Long}: 频道 id。 */
    public static final CPKey<Long> CHANNEL_MEMBER_INFO_CID = CPKey.of("ChannelMemberInfo_Cid", Long.class);
    /** {@code String}: 成员在频道内的昵称/显示名。 */
    public static final CPKey<String> CHANNEL_MEMBER_INFO_NAME = CPKey.of("ChannelMemberInfo_Name", String.class);
    /** {@code Integer}: 权限枚举值（owner/admin/member 等，见对应枚举）。 */
    public static final CPKey<Integer> CHANNEL_MEMBER_INFO_AUTHORITY = CPKey.of("ChannelMemberInfo_Authority", Integer.class);
    /** {@code String}: 申请/备注信息等（按链路语义写入）。 */
    public static final CPKey<String> CHANNEL_MEMBER_INFO_MSG = CPKey.of("ChannelMemberInfo_Msg", String.class);
    /** LocalDateTime: 成员加入时间 */
    public static final CPKey<LocalDateTime> CHANNEL_MEMBER_INFO_JOIN_TIME = CPKey.of("ChannelMemberInfo_JoinTime", LocalDateTime.class);
    /** {@code List<CPChannelMember>}: 成员列表。 */
    public static final CPKey<Set> CHANNEL_MEMBER_INFO_LIST = CPKey.of("members", Set.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeChannelMemberKeys() {
    }
}
