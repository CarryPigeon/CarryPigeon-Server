package team.carrypigeon.backend.chat.domain.features.user.application.dto;

import java.util.List;

/**
 * 用户资料分页结果。
 * 职责：承载用户资料分页查询与搜索的结果集合。
 * 边界：只表达分页数据，不承载 HTTP 协议字段。
 *
 * @param users 用户资料列表
 * @param nextCursor 下一页游标，可为空
 */
public record UserProfilePageResult(List<UserProfileResult> users, Long nextCursor) {
}
