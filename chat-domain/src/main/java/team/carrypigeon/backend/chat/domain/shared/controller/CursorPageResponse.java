package team.carrypigeon.backend.chat.domain.shared.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
/**
 * 通用游标分页响应。
 * 职责：为分页 HTTP 接口提供统一的 items / next_cursor / has_more 结构。
 * 边界：只表达协议层分页外壳，不承载具体业务语义。
 *
 * @param items 当前页数据
 * @param nextCursor 下一页游标
 * @param hasMore 是否还有更多数据
 * @param <T> 当前页元素类型
 */
@Schema(description = "通用游标分页响应。")
public record CursorPageResponse<T>(
        @Schema(description = "当前页数据")
        List<T> items,
        @Schema(description = "下一页游标；为空表示没有更多数据", example = "Y2hhbm5lbF9tZXNzYWdlczo5OTg")
        String nextCursor,
        @Schema(description = "是否还有更多数据", example = "true")
        boolean hasMore
) {

    public static <T> CursorPageResponse<T> of(List<T> items, Long nextCursor) {
        return of(items, nextCursor == null ? null : String.valueOf(nextCursor), nextCursor != null);
    }

    public static <T> CursorPageResponse<T> of(List<T> items, Long nextCursor, boolean hasMore) {
        return of(items, nextCursor == null ? null : String.valueOf(nextCursor), hasMore);
    }

    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor) {
        return of(items, nextCursor, nextCursor != null);
    }

    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new CursorPageResponse<>(
                items,
                nextCursor,
                hasMore
        );
    }
}
