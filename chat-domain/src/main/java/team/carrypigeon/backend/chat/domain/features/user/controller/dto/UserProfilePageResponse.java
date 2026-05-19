package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 用户资料分页响应。
 * 职责：对外返回用户资料分页查询与搜索的稳定结果。
 * 边界：只表达协议层分页结果，不暴露服务实现细节。
 *
 * @param users 用户资料列表
 * @param nextCursor 下一页游标，可为空
 */
public record UserProfilePageResponse(
        @Schema(description = "当前页用户资料列表")
        List<UserProfileResponse> users,
        @Schema(description = "下一页游标；为空表示没有更多数据", example = "998")
        Long nextCursor
) {
}
