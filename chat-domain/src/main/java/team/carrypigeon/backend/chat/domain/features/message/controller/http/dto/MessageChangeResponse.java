package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;
import java.util.Map;

/**
 * 频道消息变更恢复响应。
 *
 * @param items 按事件顺序排列的结果；省略请求游标时为空
 * @param syncCursor 下一次补拉使用的游标；空页也会返回
 * @param hasMore 是否还有未读取的事件
 */
public record MessageChangeResponse(List<Event> items, String syncCursor, boolean hasMore) {
    /**
     * 一条消息变更；payload 仅含频道和消息的字符串 ID，不包含消息正文。
     *
     * @param eventId 事件 ID，用于去重
     * @param sequence 本进程的事件位置，不能以推送中的最大值跳过补拉
     * @param eventType 事件类型，message.created 表示创建，message.recalled 表示撤回
     * @param serverTime 事件产生时间，epoch 毫秒
     * @param payload 包含字符串类型的 channel_id 和 message_id
     */
    public record Event(String eventId, long sequence, String eventType, long serverTime, Map<String, String> payload) { }
}
