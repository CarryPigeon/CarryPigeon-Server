package team.carrypigeon.backend.chat.domain.features.user.application.query;

/**
 * 搜索用户资料命令。
 * 职责：承载用户资料搜索用例的最小输入。
 * 边界：只携带查询标识、关键字、游标与分页条数，不包含展示规则。
 *
 * @param accountId 当前登录账户 ID
 * @param keyword 搜索关键字
 * @param cursorAccountId 游标账户 ID，可为空
 * @param limit 查询条数
 */
public record SearchUserProfilesQuery(long accountId, String keyword, Long cursorAccountId, int limit) {
}
