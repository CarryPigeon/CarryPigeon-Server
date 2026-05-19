package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户响应。
 * 职责：对外返回当前 access token 对应的最小账户信息。
 * 边界：不暴露角色、权限或 refresh session 状态。
 *
 * @param accountId 当前账户 ID
 * @param username 当前用户名
 */
public record CurrentUserResponse(
        @Schema(description = "当前账户 ID", example = "1001")
        long accountId,
        @Schema(description = "当前用户名", example = "carry_user")
        String username
) {
}
