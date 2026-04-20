package team.carrypigeon.backend.chat.domain.shared.controller;

/**
 * 对外统一响应模型。
 * 职责：为 HTTP 接口提供稳定的响应码、消息与数据承载结构。
 * 边界：这里只表达协议层统一返回，不承载具体业务规则。
 *
 * @param code 稳定响应码，当前阶段固定使用 100/200/300/404/500
 * @param message 面向调用方的稳定消息
 * @param data 当前响应的数据载荷
 */
public record CPResponse<T>(int code, String message, T data) {

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
