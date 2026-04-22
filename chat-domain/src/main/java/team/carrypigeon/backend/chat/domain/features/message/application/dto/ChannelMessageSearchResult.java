package team.carrypigeon.backend.chat.domain.features.message.application.dto;

import java.util.List;

/**
 * 频道消息搜索结果。
 * 职责：向协议层暴露频道内关键字搜索命中的消息列表。
 * 边界：不承载高亮、评分或分页游标等扩展语义。
 *
 * @param messages 搜索命中消息列表
 */
public record ChannelMessageSearchResult(List<ChannelMessageResult> messages) {
}
