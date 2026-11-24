package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.util.List;

/**
 * 数据库服务查询得到的一页记录及总数。
 * 职责：承载当前页数据、页码、页大小与总记录数，并提供是否还有下一页的派生判断。
 * 边界：不承载协议字段名与业务语义；当前页数据必须已经由数据库分页查询得到。
 *
 * @param items 当前页数据
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页条数
 * @param total 满足查询条件的总记录数
 * @param <T> 当前页元素类型
 */
public record DatabasePageResult<T>(
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
