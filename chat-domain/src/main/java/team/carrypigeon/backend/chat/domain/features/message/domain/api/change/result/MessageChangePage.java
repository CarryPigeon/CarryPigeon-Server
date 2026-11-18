package team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;

/**
 * 频道消息变更分页结果。
 *
 * @param items 按事件顺序排列的结果
 * @param nextCursor 下一次读取使用的游标，空页也会返回
 * @param hasMore 是否还有未读取的事件
 */
public record MessageChangePage(List<MessageChangeEvent> items, MessageChangeCursor nextCursor, boolean hasMore) {
}
