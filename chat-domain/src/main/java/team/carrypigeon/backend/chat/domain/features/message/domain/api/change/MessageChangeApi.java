package team.carrypigeon.backend.chat.domain.features.message.domain.api.change;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangePage;

/** 频道短期消息变更恢复能力；只向当前频道成员提供事件。 */
public interface MessageChangeApi {

    /**
     * 获取频道短期变更；不提供游标时记录调用时的最新事件位置，不返回该位置之前的事件。
     * 有游标时按事件顺序读取，游标过期、所属实例改变或事件被截断时要求重新刷新。
     *
     * @param accountId 当前账号 ID
     * @param channelId 目标频道 ID
     * @param cursor 上一次返回的游标；null 表示记录当前事件位置
     * @param limit 每页最大事件数，须为正数
     * @return 当前页、后续游标及是否还有更多事件
     */
    MessageChangePage getChanges(long accountId, long channelId, MessageChangeCursor cursor, int limit);
}
