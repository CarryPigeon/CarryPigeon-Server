package team.carrypigeon.backend.chat.domain.features.message.application.dto;

import java.util.List;

/**
 * 频道历史消息查询结果。
 * 职责：向协议层暴露历史消息列表与下一页游标。
 * 边界：不承载协议包装逻辑。
 *
 * @param messages 历史消息列表
 * @param nextCursor 下一页游标，可为空
 */
public record ChannelMessageHistoryResult(List<ChannelMessageResult> messages, Long nextCursor) {
}
