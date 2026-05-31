package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 提及列表响应。
 */
public record MentionListResponse(
        @Schema(description = "列表项") List<MentionItemResponse> items,
        @Schema(description = "下一页游标") String nextCursor,
        @Schema(description = "是否还有更多") boolean hasMore
) {
}
