package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import java.util.List;

/**
 * 频道历史消息协议响应。
 * 职责：向 HTTP 调用方暴露历史消息列表与下一页游标。
 * 边界：不承载统一响应包装逻辑。
 *
 * @param messages 历史消息列表
 * @param nextCursor 下一页游标，可为空
 */
public record ChannelMessageHistoryResponse(List<ChannelMessageResponse> messages, Long nextCursor) {
}
