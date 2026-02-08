package team.carrypigeon.backend.chat.domain.cmp.info;

/**
 * 分页信息结构（LiteFlow/Result 复用）。
 *
 * @param page 页码（从 1 开始的约定由业务链路决定）
 * @param pageSize 每页数量
 */
public record PageInfo(int page, int pageSize) {
}
