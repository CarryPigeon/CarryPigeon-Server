package team.carrypigeon.backend.chat.domain.features.user.domain.query;

/**
 * 按账户 ID 查询用户资料的查询对象。
 * 职责：承载用户资料读取用例的最小查询条件。
 * 边界：只携带查询标识，不包含展示或编辑规则。
 *
 * @param accountId 目标账户 ID
 */
public record GetUserProfileByAccountIdQuery(long accountId) {
}
