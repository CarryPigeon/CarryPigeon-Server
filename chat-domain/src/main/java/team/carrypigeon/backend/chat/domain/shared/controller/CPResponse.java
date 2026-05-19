package team.carrypigeon.backend.chat.domain.shared.controller;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 对外统一响应模型。
 * 职责：为 HTTP 接口提供稳定的响应码、消息与数据承载结构。
 * 边界：这里只表达协议层统一返回，不承载具体业务规则。
 *
 * @param code 稳定响应码，当前阶段固定使用 100/200/300/404/500
 * @param message 面向调用方的稳定消息
 * @param data 当前响应的数据载荷
 */
@Schema(description = "统一 HTTP 响应包装。调用方应同时读取 HTTP 状态码与此对象中的稳定业务码。")
public record CPResponse<T>(
        @Schema(description = "稳定业务响应码：100=成功，200=参数或请求体错误，300=认证或权限失败，404=资源不存在，500=服务内部错误", example = "100")
        int code,
        @Schema(description = "面向调用方的稳定响应消息", example = "success")
        String message,
        @Schema(description = "业务数据载荷；失败时通常为 null")
        T data
) {

    public static <T> CPResponse<T> success(T data) {
        return new CPResponse<>(100, "success", data);
    }

    public static <T> CPResponse<T> validationFailed(String message) {
        return new CPResponse<>(200, message, null);
    }

    public static <T> CPResponse<T> forbidden(String message) {
        return new CPResponse<>(300, message, null);
    }

    public static <T> CPResponse<T> notFound(String message) {
        return new CPResponse<>(404, message, null);
    }

    public static <T> CPResponse<T> fail(String message) {
        return new CPResponse<>(500, message, null);
    }
}
