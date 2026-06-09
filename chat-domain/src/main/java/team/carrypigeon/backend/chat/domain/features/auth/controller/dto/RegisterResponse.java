package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 注册响应。
 * 职责：向客户端返回新建账户的最小稳定标识。
 * 边界：不承载令牌、资料详情或敏感字段。
 */
public record RegisterResponse(
        @Schema(description = "账户 ID", example = "1001")
        String uid,
        @Schema(description = "用户名", example = "carry-user")
        String username
) {
}
