package team.carrypigeon.backend.chat.domain.controller.web.api.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 错误响应主体。
 * <p>
 * 作为 `ApiErrorResponse#error` 字段的值承载标准错误语义。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorBody {

    /**
     * HTTP 状态码。
     */
    private int status;

    /**
     * 机器可解析错误码。
     */
    private String reason;

    /**
     * 人类可读错误描述。
     */
    private String message;

    /**
     * 请求追踪 ID。
     */
    private String requestId;

    /**
     * 结构化错误明细。
     */
    private Object details;
}
