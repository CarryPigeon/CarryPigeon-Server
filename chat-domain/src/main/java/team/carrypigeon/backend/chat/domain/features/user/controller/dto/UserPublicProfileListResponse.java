package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 用户公开资料列表响应。
 * 职责：承载 `GET /api/users?ids=...` 的 items 外壳。
 * 边界：只表达列表结构，不承载分页语义。
 */
public record UserPublicProfileListResponse(
        @Schema(description = "公开资料列表")
        List<UserPublicProfileResponse> items
) {
}
