package team.carrypigeon.backend.chat.domain.features.user.domain.query;

/**
 * 查询用户资料分页命令。
 * 职责：承载用户资料分页查询用例的最小输入。
 * 边界：只携带查询标识、游标与分页条数，不包含展示规则。
 *
 * @param accountId 当前登录账户 ID
 * @param cursorAccountId 游标账户 ID，可为空
 * @param limit 查询条数
 */
public record GetUserProfilesQuery(long accountId, Long cursorAccountId, int limit) {
}
