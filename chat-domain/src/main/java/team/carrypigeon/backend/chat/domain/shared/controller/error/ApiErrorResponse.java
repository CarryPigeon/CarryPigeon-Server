package team.carrypigeon.backend.chat.domain.shared.controller.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准 HTTP 错误响应包装。
 * 职责：为所有非 2xx HTTP 响应提供统一 `error` 外层结构。
 * 边界：不承载成功响应模型。
 *
 * @param error 标准错误对象
 */
@Schema(description = "标准 HTTP 错误响应包装。")
public record ApiErrorResponse(
        @Schema(description = "错误对象")
        ApiError error
) {
}
