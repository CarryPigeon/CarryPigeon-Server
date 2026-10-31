package team.carrypigeon.backend.infrastructure.basic.page;

import java.util.List;

/**
 * 通用游标分页响应。
 * 职责：为 message feature 的游标分页接口提供统一的 items / next_cursor / has_more 结构。
 * 边界：只表达游标分页外壳，不承载具体业务语义；页码分页使用 {@link PageResult}。
 *
 * @param items 当前页数据
 * @param nextCursor 下一页游标
 * @param hasMore 是否还有更多数据
 * @param <T> 当前页元素类型
 */
public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {

    /**
     * 基于数值游标构造分页响应，并按游标是否为空推导是否还有下一页。
     *
     * @param items 当前页数据
     * @param nextCursor 下一页数值游标；为空表示没有更多数据
     * @param <T> 当前页元素类型
     * @return 标准游标分页响应
     */
    public static <T> CursorPageResponse<T> of(List<T> items, Long nextCursor) {
        return of(items, nextCursor == null ? null : String.valueOf(nextCursor), nextCursor != null);
    }

    /**
     * 基于数值游标与显式 hasMore 标记构造分页响应。
     *
     * @param items 当前页数据
     * @param nextCursor 下一页数值游标；为空时响应中的 next_cursor 也为空
     * @param hasMore 是否还有更多数据
     * @param <T> 当前页元素类型
     * @return 标准游标分页响应
     */
    public static <T> CursorPageResponse<T> of(List<T> items, Long nextCursor, boolean hasMore) {
        return of(items, nextCursor == null ? null : String.valueOf(nextCursor), hasMore);
    }

    /**
     * 基于字符串游标构造分页响应，并按游标是否为空推导是否还有下一页。
     *
     * @param items 当前页数据
     * @param nextCursor 下一页字符串游标；为空表示没有更多数据
     * @param <T> 当前页元素类型
     * @return 标准游标分页响应
     */
    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor) {
        return of(items, nextCursor, nextCursor != null);
    }

    /**
     * 构造完整的游标分页响应。
     *
     * @param items 当前页数据
     * @param nextCursor 下一页字符串游标
     * @param hasMore 是否还有更多数据
     * @param <T> 当前页元素类型
     * @return 标准游标分页响应
     */
    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new CursorPageResponse<>(items, nextCursor, hasMore);
    }
}
