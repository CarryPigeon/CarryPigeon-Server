package team.carrypigeon.backend.chat.domain.config.http.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import team.carrypigeon.backend.chat.domain.config.http.error.ApiError;
import team.carrypigeon.backend.chat.domain.config.http.error.ApiErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * 安全层统一错误响应写入器。
 * 职责：让 Spring Security 的 401/403/500 与其他 HTTP 错误使用同一 {@link ApiErrorResponse} 结构。
 * 边界：只负责安全层错误响应，不承载认证或授权判断。
 */
final class SecurityErrorResponses {

    private SecurityErrorResponses() {
    }

    /**
     * 写入标准错误响应。
     *
     * @param jsonProvider JSON 序列化入口
     * @param response HTTP 响应
     * @param status HTTP 状态
     * @param reason 稳定失败原因
     * @param message 客户端可读消息
     * @param details 可选错误明细
     */
    static void write(
            JsonProvider jsonProvider,
            HttpServletResponse response,
            int status,
            String reason,
            String message,
            Map<String, Object> details
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        ApiError error = new ApiError(
                status,
                reason,
                message,
                LogContexts.currentSessionId(),
                details
        );
        response.getWriter().write(jsonProvider.toJson(new ApiErrorResponse(error)));
    }
}
