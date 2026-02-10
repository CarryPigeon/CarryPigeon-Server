package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.util.List;

/**
 * 用户相关上下文 key 封装（UserInfo_*）。
 */
public final class CPNodeUserKeys {

    /** {@code CPUser}: 用户实体。 */
    public static final CPKey<CPUser> USER_INFO = CPKey.of("UserInfo", CPUser.class);
    /** {@code Long}: 用户 id。 */
    public static final CPKey<Long> USER_INFO_ID = CPKey.of("UserInfo_Id", Long.class);
    /** {@code String}: 用户昵称/用户名。 */
    public static final CPKey<String> USER_INFO_USER_NAME = CPKey.of("UserInfo_UserName", String.class);
    /** {@code String}: 用户邮箱。 */
    public static final CPKey<String> USER_INFO_EMAIL = CPKey.of("UserInfo_Email", String.class);
    /** {@code Integer}: 性别枚举值（见 {@code CPUserSexEnum}）。 */
    public static final CPKey<Integer> USER_INFO_SEX = CPKey.of("UserInfo_Sex", Integer.class);
    /** {@code Long}: 生日时间戳（毫秒）。 */
    public static final CPKey<Long> USER_INFO_BIRTHDAY = CPKey.of("UserInfo_Birthday", Long.class);
    /** {@code Long}: 注册时间（毫秒时间戳）。 */
    public static final CPKey<Long> USER_INFO_REGISTER_TIME = CPKey.of("UserInfo_RegisterTime", Long.class);
    /** {@code String}: 简介。 */
    public static final CPKey<String> USER_INFO_BRIEF = CPKey.of("UserInfo_Brief", String.class);
    /** {@code Long}: 头像文件 id。 */
    public static final CPKey<Long> USER_INFO_AVATAR = CPKey.of("UserInfo_Avatar", Long.class);

    /** {@code List<Long>}: 用户 id 列表（用于 batch 查询）。 */
    public static final CPKey<List> USER_INFO_ID_LIST = CPKey.of("UserInfo_IdList", List.class);
    /** {@code List<CPUser>}: 用户列表（用于 batch 返回）。 */
    public static final CPKey<List> USER_INFO_LIST = CPKey.of("UserInfo_List", List.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeUserKeys() {
    }
}
