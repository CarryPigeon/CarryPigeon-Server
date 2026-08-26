package team.carrypigeon.backend.chat.domain.features.user.domain.query;

/**
 * 当前用户资料查询。
 * 职责：承载当前登录用户资料查询用例需要的最小输入。
 * 边界：只表达读取条件，不承载协议注解与持久化细节。
 *
 * @param accountId 当前登录账户 ID
 */
public record GetCurrentUserProfileQuery(long accountId) {
}
