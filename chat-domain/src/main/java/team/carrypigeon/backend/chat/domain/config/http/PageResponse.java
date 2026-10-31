package team.carrypigeon.backend.chat.domain.config.http;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

/**
 * 通用页码分页响应。
 * 职责：为分页 HTTP 接口提供统一的 items / page / page_size / total / has_more 结构。
 * 边界：只表达协议层分页外壳，不承载具体业务语义。
 *
 * @param items 当前页数据
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页条数
 * @param total 满足查询条件的总记录数
 * @param hasMore 是否还有下一页
 * @param <T> 当前页元素类型
 */
@Schema(description = "通用页码分页响应。")
public record PageResponse<T>(
        @Schema(description = "当前页数据")
        List<T> items,
        @Schema(description = "当前页码，从 1 开始", example = "1")
        int page,
        @Schema(description = "每页条数", example = "20")
        int pageSize,
        @Schema(description = "满足查询条件的总记录数", example = "57")
        long total,
        @Schema(description = "是否还有下一页", example = "true")
        boolean hasMore
) {

    /**
     * 从领域分页结果构造协议响应。
     *
     * @param result 领域分页结果
     * @param <T> 当前页元素类型
     * @return 协议分页响应
     */
    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.items(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        );
    }
}
