package team.carrypigeon.backend.infrastructure.basic.page;

/**
 * 通用页码分页查询参数。
 * 职责：表达从 1 开始的页码和每页条数，并给出数据库偏移量。
 * 边界：只承载分页位置与数量，不承载业务过滤条件；页码与页大小的合法性由协议层校验。
 *
 * @param page 页码，从 1 开始
 * @param pageSize 每页条数
 */
public record PageQuery(int page, int pageSize) {

    /**
     * 计算 SQL 偏移量。
     *
     * @return 从 0 开始的偏移量
     */
    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}
