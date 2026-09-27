package team.carrypigeon.backend.chat.domain.features.user.domain.model;

import java.time.Instant;

/**
 * 用户资料领域模型。
 * 职责：表达已持久化的当前用户资料语义。
 * 边界：不承载鉴权口令、令牌或角色权限语义。
 *
 * @param accountId 关联鉴权账户 ID
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param sex 用户性别协议值
 * @param birthday 用户生日协议值
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfile(
        long accountId,
        String avatarUrl,
        String bio,
        long sex,
        long birthday,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * 创建新注册用户的默认资料。
     *
     * @param accountId 账户 ID
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 默认资料模型
     */
    public static UserProfile initial(long accountId, Instant createdAt, Instant updatedAt) {
        return new UserProfile(accountId, "", "", 0L, 0L, createdAt, updatedAt);
    }

    /**
     * 生成更新后的资料副本。
     *
     * @param avatarUrl 新头像地址
     * @param bio 新简介
     * @param sex 新性别协议值
     * @param birthday 新生日协议值
     * @param updatedAt 更新时间
     * @return 更新后的资料模型
     */
    public UserProfile updateProfile(String avatarUrl, String bio, long sex, long birthday, Instant updatedAt) {
        return new UserProfile(accountId, avatarUrl, bio, sex, birthday, createdAt, updatedAt);
    }
}
