package team.carrypigeon.backend.chat.domain.shared.page;

/**
 * 业务列表查询的分页范围，筛选条件由具体用例提供。
 *
 * @param page 页码，从 1 开始
 * @param pageSize 每页条数
 */
public record PageQuery(int page, int pageSize) {
}
