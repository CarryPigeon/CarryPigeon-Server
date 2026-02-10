package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import java.util.List;

/**
 * 批量用户查询内部请求。
 *
 * @param ids 用户 ID 列表。
 */
public record UsersBatchRequest(List<String> ids) {
}
