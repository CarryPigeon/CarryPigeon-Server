package team.carrypigeon.backend.chat.domain.shared.page;

import java.util.List;

/**
 * 业务列表查询的分页结果。
 * 职责：承载当前页数据、页码、页大小与总记录数，并提供是否还有下一页的派生判断。
 *
 * @param items 当前页数据
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页条数
 * @param total 满足查询条件的总记录数
 * @param <T> 当前页元素类型
 */
public record PageResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
) {

    /**
     * 判断是否还有下一页。
     *
     * @return 当前页之后仍有记录时返回 true
     */
    public boolean hasMore() {
        return (long) page * pageSize < total;
    }
}
