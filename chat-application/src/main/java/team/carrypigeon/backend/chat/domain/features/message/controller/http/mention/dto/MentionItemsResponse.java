package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto;

import java.util.List;

/** 批量查询命中的可见提及，按请求中的首次出现顺序排列。 */
public record MentionItemsResponse(List<MentionItemResponse> items) {
}
