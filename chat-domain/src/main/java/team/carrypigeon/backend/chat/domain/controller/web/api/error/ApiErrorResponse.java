package team.carrypigeon.backend.chat.domain.controller.web.api.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 错误响应包装体。
 * <p>
 * 保证所有失败响应统一为 `{"error": {...}}` 结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    /**
     * 错误主体。
     */
    private ApiErrorBody error;
}
