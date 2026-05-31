package team.carrypigeon.backend.chat.domain.shared.controller.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 标准 HTTP 错误对象。
 * 职责：承载稳定的 HTTP 错误状态、原因、消息与可选细节。
 * 边界：这里只表达协议层错误结构，不承载业务处理逻辑。
 *
 * @param status HTTP 状态码镜像
 * @param reason 机器可读错误原因
 * @param message 面向调用方的稳定错误消息
 * @param requestId 请求链路 ID
 * @param details 可选错误细节
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "标准错误对象。")
public record ApiError(
        @Schema(description = "HTTP 状态码镜像", example = "422")
        int status,
        @Schema(description = "机器可读错误原因", example = "validation_failed")
        String reason,
        @Schema(description = "面向调用方的稳定错误消息", example = "validation failed")
        String message,
        @Schema(description = "请求链路 ID", example = "req-01HXYZ")
        String requestId,
        @Schema(description = "可选错误细节。")
        Map<String, Object> details
) {
}
